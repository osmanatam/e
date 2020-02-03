package dev.akif.e.circe

import dev.akif.e.AbstractDecoder.DecodingResult
import dev.akif.e.scala.{Codec, E}
import io.circe.syntax._
import io.circe.{Json, JsonObject}

object CodecForCirceJson extends Codec[Json] {
  override def decode(json: Json): DecodingResult[E] =
    json.as[JsonObject] match {
      case Left(cause) =>
        val e = E(
          name    = "decoding-failure",
          message = "Input is not a Json object!",
          cause   = Some(cause),
          data    = Map("input" -> json.toString())
        )

        DecodingResult.fail(e)

      case Right(obj) =>
        val code    = obj("code").flatMap(_.as[Int].toOption).getOrElse(0)
        val name    = obj("name").flatMap(_.asString).getOrElse("")
        val message = obj("message").flatMap(_.asString).getOrElse("")
        val data    = obj("data").flatMap(_.as[Map[String, String]].toOption).getOrElse(Map.empty)

        // Cannot know cause field because it isn't possible to construct the causing exception from just a serialized message string
        DecodingResult.succeed(E(code, name, message, None, data))
    }

  override def encode(e: E): Json = {
    val jsons = List(
      if (!e.hasCode)    None else Some(Json.obj("code"    := e.code)),
      if (!e.hasName)    None else Some(Json.obj("name"    := e.name)),
      if (!e.hasMessage) None else Some(Json.obj("message" := e.message)),
      e.cause.fold[Option[Json]](None)(c => Some(Json.obj("cause" := c.getMessage))),
      if (!e.hasData)    None else Some(Json.obj("data"    := e.data))
    )

    jsons.foldLeft(Json.obj()) {
      case (result, Some(json)) => result deepMerge json
      case (result, _)          => result
    }
  }
}
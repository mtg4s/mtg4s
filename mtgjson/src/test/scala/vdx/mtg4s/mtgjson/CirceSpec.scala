package vdx.mtg4s.mtgjson

import io.circe._
import io.circe.parser.parse

trait CirceSpec {
  def decoderOf[A](implicit ev: Decoder[A]): Decoder[A] = ev

  def decode[A: Decoder](json: String): Either[Error, A] = {
    val parsed: Json = parse(json).getOrElse(Json.Null)
    val result = parsed.as[A]

    result.swap.getOrElse(DecodingFailure("", List.empty)) match {
      case d: DecodingFailure if d.history.length > 1 =>
        println(HCursor.fromJson(parsed).replay(d.history.tail).focus.get)
      case d: DecodingFailure if d.history.length == 1 =>
        println(HCursor.fromJson(parsed).replayOne(d.history(0)).focus.get)
      case _ => ()
    }

    result
  }
}

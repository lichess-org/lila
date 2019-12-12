package lila.common

import play.api.libs.json._

object PimpedJson {

  def anyValWriter[O, A: Writes](f: O => A) = Writes[O] { o =>
    Json toJson f(o)
  }

  def intAnyValWriter[O](f: O => Int): Writes[O] = anyValWriter[O, Int](f)
  def stringAnyValWriter[O](f: O => String): Writes[O] = anyValWriter[O, String](f)

  def stringIsoWriter[O](iso: Iso[String, O]): Writes[O] = anyValWriter[O, String](iso.to)
  def intIsoWriter[O](iso: Iso[Int, O]): Writes[O] = anyValWriter[O, Int](iso.to)

  def stringIsoReader[O](iso: Iso[String, O]): Reads[O] = Reads.of[String] map iso.from

  def intIsoFormat[O](iso: Iso[Int, O]): Format[O] = Format[O](
    Reads.of[Int] map iso.from,
    Writes { o => JsNumber(iso to o) }
  )

  def stringIsoFormat[O](iso: Iso[String, O]): Format[O] = Format[O](
    Reads.of[String] map iso.from,
    Writes { o => JsString(iso to o) }
  )

  implicit val centisReads = Reads.of[Int] map chess.Centis.apply
}

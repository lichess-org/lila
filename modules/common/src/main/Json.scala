package lila.common

import org.joda.time.DateTime
import play.api.libs.json.{ Json => PlayJson, _ }
import chess.format.{ FEN, Uci }

object Json {

  def anyValWriter[O, A: Writes](f: O => A) =
    Writes[O] { o =>
      PlayJson toJson f(o)
    }

  def intAnyValWriter[O](f: O => Int): Writes[O]       = anyValWriter[O, Int](f)
  def stringAnyValWriter[O](f: O => String): Writes[O] = anyValWriter[O, String](f)

  def stringIsoWriter[O](iso: Iso[String, O]): Writes[O] = anyValWriter[O, String](iso.to)
  def intIsoWriter[O](iso: Iso[Int, O]): Writes[O]       = anyValWriter[O, Int](iso.to)

  def stringIsoReader[O](iso: Iso[String, O]): Reads[O] = Reads.of[String] map iso.from

  def intIsoFormat[O](iso: Iso[Int, O]): Format[O] =
    Format[O](
      Reads.of[Int] map iso.from,
      Writes { o =>
        JsNumber(iso to o)
      }
    )

  def stringIsoFormat[O](iso: Iso[String, O]): Format[O] =
    Format[O](
      Reads.of[String] map iso.from,
      Writes { o =>
        JsString(iso to o)
      }
    )

  def stringRead[O](from: String => O): Reads[O] = Reads.of[String] map from

  def optRead[O](from: String => Option[O]): Reads[O] = Reads.of[String].flatMapResult { str =>
    from(str).fold[JsResult[O]](JsError(s"Invalid value: $str"))(JsSuccess(_))
  }
  def optFormat[O](from: String => Option[O], to: O => String): Format[O] = Format[O](
    optRead(from),
    Writes(o => JsString(to(o)))
  )

  def tryRead[O](from: String => scala.util.Try[O]): Reads[O] = Reads.of[String].flatMapResult { code =>
    from(code).fold(err => JsError(err.getMessage), JsSuccess(_))
  }
  def tryFormat[O](from: String => scala.util.Try[O], to: O => String): Format[O] = Format[O](
    tryRead(from),
    Writes[O](o => JsString(to(o)))
  )

  implicit val centisReads = Reads.of[Int] map chess.Centis.apply

  implicit val jodaWrites = Writes[DateTime] { time =>
    JsNumber(time.getMillis)
  }

  implicit val colorWrites: Writes[chess.Color] = Writes { c =>
    JsString(c.name)
  }

  implicit val fenFormat: Format[FEN]           = stringIsoFormat[FEN](Iso.fenIso)
  implicit val markdownFormat: Format[Markdown] = stringIsoFormat[Markdown](Iso.markdownIso)

  implicit val uciReads: Reads[Uci] = Reads.of[String] flatMapResult { str =>
    JsResult.fromTry(Uci(str) toTry s"Invalid UCI: $str")
  }
  implicit val uciWrites: Writes[Uci] = Writes { u =>
    JsString(u.uci)
  }

  implicit def openingFamilyReads = Reads[LilaOpeningFamily] { f =>
    f.get[String]("key")
      .flatMap(LilaOpeningFamily.find)
      .fold[JsResult[LilaOpeningFamily]](JsError(Nil))(JsSuccess(_))
  }
}

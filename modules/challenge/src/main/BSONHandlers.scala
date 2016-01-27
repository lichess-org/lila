package lila.challenge

import reactivemongo.bson._

import chess.Mode
import chess.variant.Variant
import lila.db.BSON
import lila.db.BSON._
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.Implicits._
import lila.rating.PerfType

private object BSONHandlers {

  import Challenge._

  implicit val ColorChoiceBSONHandler = new BSONHandler[BSONInteger, ColorChoice] {
    def read(b: BSONInteger) = b.value match {
      case 1 => ColorChoice.White
      case 2 => ColorChoice.Black
      case _ => ColorChoice.Random
    }
    def write(c: ColorChoice) = BSONInteger(c match {
      case ColorChoice.White  => 1
      case ColorChoice.Black  => 2
      case ColorChoice.Random => 0
    })
  }
  implicit val TimeControlBSONHandler = new BSON[TimeControl] {
    def reads(r: Reader) = (r.intO("l") |@| r.intO("i")) {
      case (limit, inc) => TimeControl.Clock(limit, inc)
    } orElse {
      r intO "d" map TimeControl.Correspondence.apply
    } getOrElse TimeControl.Unlimited
    def writes(w: Writer, t: TimeControl) = t match {
      case TimeControl.Clock(l, i)       => BSONDocument("l" -> l, "i" -> i)
      case TimeControl.Correspondence(d) => BSONDocument("d" -> d)
      case TimeControl.Unlimited         => BSONDocument()
    }
  }
  implicit val VariantBSONHandler = new BSONHandler[BSONInteger, Variant] {
    def read(b: BSONInteger): Variant = Variant(b.value) err s"No such variant: ${b.value}"
    def write(x: Variant) = BSONInteger(x.id)
  }
  implicit val ModeBSONHandler = new BSONHandler[BSONBoolean, Mode] {
    def read(b: BSONBoolean) = Mode(b.value)
    def write(m: Mode) = BSONBoolean(m.rated)
  }
  implicit val EitherChallengerBSONHandler = new BSON[EitherChallenger] {
    def reads(r: Reader) = (r.strO("id") |@| r.intO("rating")) {
      case (id, rating) => Right(Registered(id, rating))
    } orElse r.strO("secret").map { secret =>
      Left(Anonymous(secret))
    } err s"Can't read challenger ${r.debug}"
    def writes(w: Writer, c: EitherChallenger) = c.fold(
      a => BSONDocument("secret" -> a.secret),
      r => BSONDocument("id" -> r.id, "rating" -> r.rating))
  }

  implicit val ChallengeBSONHandler = Macros.handler[Challenge]
}


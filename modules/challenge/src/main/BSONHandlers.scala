package lila.challenge

import reactivemongo.api.bson._

import shogi.Mode
import shogi.variant.Variant
import lila.db.BSON
import lila.db.BSON.{ Reader, Writer }
import lila.db.dsl._

private object BSONHandlers {

  import Challenge._

  implicit val ColorChoiceBSONHandler = BSONIntegerHandler.as[ColorChoice](
    {
      case 1 => ColorChoice.Sente
      case 2 => ColorChoice.Gote
      case _ => ColorChoice.Random
    },
    {
      case ColorChoice.Sente  => 1
      case ColorChoice.Gote   => 2
      case ColorChoice.Random => 0
    }
  )
  implicit val TimeControlBSONHandler = new BSON[TimeControl] {
    import cats.implicits._
    def reads(r: Reader) =
      (r.intO("l"), r.intO("i"), Some(r.intD("b")), Some(r.intD("p"))) mapN { (limit, inc, byo, per) =>
        {
          TimeControl.Clock(shogi.Clock.Config(limit, inc, byo, per))
        }
      } orElse {
        r intO "d" map TimeControl.Correspondence.apply
      } getOrElse TimeControl.Unlimited
    def writes(w: Writer, t: TimeControl) =
      t match {
        case TimeControl.Clock(shogi.Clock.Config(l, i, b, p)) => $doc("l" -> l, "i" -> i, "b" -> b, "p" -> p)
        case TimeControl.Correspondence(d)                     => $doc("d" -> d)
        case TimeControl.Unlimited                             => $empty
      }
  }
  implicit val VariantBSONHandler = tryHandler[Variant](
    { case BSONInteger(v) => Variant(v) toTry s"No such variant: $v" },
    x => BSONInteger(x.id)
  )
  implicit val StatusBSONHandler = tryHandler[Status](
    { case BSONInteger(v) => Status(v) toTry s"No such status: $v" },
    x => BSONInteger(x.id)
  )
  implicit val ModeBSONHandler = BSONBooleanHandler.as[Mode](Mode.apply, _.rated)
  implicit val RatingBSONHandler = new BSON[Rating] {
    def reads(r: Reader) = Rating(r.int("i"), r.boolD("p"))
    def writes(w: Writer, r: Rating) =
      $doc(
        "i" -> r.int,
        "p" -> w.boolO(r.provisional)
      )
  }
  implicit val RegisteredBSONHandler = new BSON[Challenger.Registered] {
    def reads(r: Reader) = Challenger.Registered(r.str("id"), r.get[Rating]("r"))
    def writes(w: Writer, r: Challenger.Registered) =
      $doc(
        "id" -> r.id,
        "r"  -> r.rating
      )
  }
  implicit val AnonymousBSONHandler = new BSON[Challenger.Anonymous] {
    def reads(r: Reader) = Challenger.Anonymous(r.str("s"))
    def writes(w: Writer, a: Challenger.Anonymous) =
      $doc(
        "s" -> a.secret
      )
  }
  implicit val ChallengerBSONHandler = new BSON[Challenger] {
    def reads(r: Reader) =
      if (r contains "id") RegisteredBSONHandler reads r
      else if (r contains "s") AnonymousBSONHandler reads r
      else Challenger.Open
    def writes(w: Writer, c: Challenger) =
      c match {
        case a: Challenger.Registered => RegisteredBSONHandler.writes(w, a)
        case a: Challenger.Anonymous  => AnonymousBSONHandler.writes(w, a)
        case _                        => $empty
      }
  }

  implicit val ChallengeBSONHandler = Macros.handler[Challenge]
}

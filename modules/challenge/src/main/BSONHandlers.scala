package lila.challenge

import reactivemongo.api.bson._

import chess.Mode
import chess.variant.Variant
import lila.db.BSON
import lila.db.BSON.{ Reader, Writer }
import lila.db.dsl._

private object BSONHandlers {

  import Challenge._

  implicit val ColorChoiceBSONHandler = BSONIntegerHandler.as[ColorChoice](
    {
      case 1 => ColorChoice.White
      case 2 => ColorChoice.Black
      case _ => ColorChoice.Random
    }, {
      case ColorChoice.White  => 1
      case ColorChoice.Black  => 2
      case ColorChoice.Random => 0
    }
  )
  implicit val TimeControlBSONHandler = new BSON[TimeControl] {
    def reads(r: Reader) =
      (r.intO("l") |@| r.intO("i")) {
        case (limit, inc) => TimeControl.Clock(chess.Clock.Config(limit, inc))
      } orElse {
        r intO "d" map TimeControl.Correspondence.apply
      } getOrElse TimeControl.Unlimited
    def writes(w: Writer, t: TimeControl) = t match {
      case TimeControl.Clock(chess.Clock.Config(l, i)) => $doc("l" -> l, "i" -> i)
      case TimeControl.Correspondence(d)               => $doc("d" -> d)
      case TimeControl.Unlimited                       => $empty
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
    def writes(w: Writer, r: Rating) = $doc(
      "i" -> r.int,
      "p" -> w.boolO(r.provisional)
    )
  }
  implicit val RegisteredBSONHandler = new BSON[Registered] {
    def reads(r: Reader) = Registered(r.str("id"), r.get[Rating]("r"))
    def writes(w: Writer, r: Registered) = $doc(
      "id" -> r.id,
      "r"  -> r.rating
    )
  }
  implicit val AnonymousBSONHandler = new BSON[Anonymous] {
    def reads(r: Reader) = Anonymous(r.str("s"))
    def writes(w: Writer, a: Anonymous) = $doc(
      "s" -> a.secret
    )
  }
  implicit val EitherChallengerBSONHandler = new BSON[EitherChallenger] {
    def reads(r: Reader) =
      if (r contains "id") Right(RegisteredBSONHandler reads r)
      else Left(AnonymousBSONHandler reads r)
    def writes(w: Writer, c: EitherChallenger) = c.fold(
      a => AnonymousBSONHandler.writes(w, a),
      r => RegisteredBSONHandler.writes(w, r)
    )
  }

  import lila.game.BSONHandlers.FENBSONHandler

  implicit val ChallengeBSONHandler = Macros.handler[Challenge]
}

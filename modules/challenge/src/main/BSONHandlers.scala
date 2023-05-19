package lila.challenge

import chess.variant.Variant
import reactivemongo.api.bson.*

import lila.common.Days
import lila.db.BSON
import lila.db.BSON.{ Reader, Writer }
import lila.db.dsl.{ *, given }

private object BSONHandlers:

  import Challenge.*
  import lila.game.BSONHandlers.given

  given BSONHandler[ColorChoice] = BSONIntegerHandler.as[ColorChoice](
    {
      case 1 => ColorChoice.White
      case 2 => ColorChoice.Black
      case _ => ColorChoice.Random
    },
    {
      case ColorChoice.White  => 1
      case ColorChoice.Black  => 2
      case ColorChoice.Random => 0
    }
  )
  given BSON[TimeControl] with
    import cats.syntax.all.*
    import chess.Clock
    def reads(r: Reader) =
      (r.getO[Clock.LimitSeconds]("l"), r.getO[Clock.IncrementSeconds]("i")) mapN { (limit, inc) =>
        TimeControl.Clock(chess.Clock.Config(limit, inc))
      } orElse {
        r.getO[Days]("d") map TimeControl.Correspondence.apply
      } getOrElse TimeControl.Unlimited
    def writes(w: Writer, t: TimeControl) =
      t match
        case TimeControl.Clock(chess.Clock.Config(l, i)) => $doc("l" -> l, "i" -> i)
        case TimeControl.Correspondence(d)               => $doc("d" -> d)
        case TimeControl.Unlimited                       => $empty
  given BSONHandler[Variant]       = variantByIdHandler
  given BSONHandler[Status]        = valueMapHandler(Status.byId)(_.id)
  given BSONHandler[DeclineReason] = valueMapHandler(DeclineReason.byKey)(_.key)

  given BSON[Rating] with
    def reads(r: Reader) = Rating(r.get("i"), r.yesnoD("p"))
    def writes(w: Writer, r: Rating) =
      $doc(
        "i" -> r.int,
        "p" -> w.boolO(r.provisional.yes)
      )
  given registeredHandler: BSON[Challenger.Registered] with
    def reads(r: Reader) = Challenger.Registered(r.get[UserId]("id"), r.get[Rating]("r"))
    def writes(w: Writer, r: Challenger.Registered) =
      $doc(
        "id" -> r.id,
        "r"  -> r.rating
      )
  given anonHandler: BSON[Challenger.Anonymous] with
    def reads(r: Reader)                           = Challenger.Anonymous(r.str("s"))
    def writes(w: Writer, a: Challenger.Anonymous) = $doc("s" -> a.secret)

  given BSON[Challenger] with
    def reads(r: Reader) =
      if (r contains "id") registeredHandler reads r
      else if (r contains "s") anonHandler reads r
      else Challenger.Open
    def writes(w: Writer, c: Challenger) =
      c match
        case a: Challenger.Registered => registeredHandler.writes(w, a)
        case a: Challenger.Anonymous  => anonHandler.writes(w, a)
        case _                        => $empty

  given BSONDocumentHandler[Challenge.Open] = Macros.handler
  given BSONDocumentHandler[Challenge]      = Macros.handler

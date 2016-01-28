package lila.challenge

import chess.variant.Variant
import chess.{ Mode, Clock, Speed }
import org.joda.time.DateTime

import lila.game.PerfPicker
import lila.rating.PerfType

case class Challenge(
    _id: String,
    variant: Variant,
    timeControl: Challenge.TimeControl,
    mode: Mode,
    color: Challenge.ColorChoice,
    challenger: EitherChallenger,
    destUserId: Option[String],
    createdAt: DateTime,
    expiresAt: DateTime) {

  def id = _id

  lazy val perfType = Challenge.perfType(variant, timeControl)
}

object Challenge {

  case class Rating(int: Int, provisional: Boolean)
  object Rating {
    def apply(p: lila.rating.Perf): Rating = Rating(p.intRating, p.provisional)
  }

  case class Registered(id: String, rating: Rating)
  case class Anonymous(secret: String)

  sealed trait TimeControl
  object TimeControl {
    case object Unlimited extends TimeControl
    case class Correspondence(days: Int) extends TimeControl
    case class Clock(limit: Int, increment: Int) extends TimeControl {
      // All durations are expressed in seconds
      def show = chessClock.show
      lazy val chessClock = chess.Clock(limit, increment)
    }
  }

  sealed trait ColorChoice
  object ColorChoice {
    case object Random extends ColorChoice
    case object White extends ColorChoice
    case object Black extends ColorChoice
  }

  def speed(timeControl: TimeControl) = timeControl match {
    case c: TimeControl.Clock => Speed(c.chessClock)
    case _                    => Speed.Correspondence
  }

  def perfType(variant: Variant, timeControl: TimeControl) =
    PerfPicker.perfType(speed(timeControl), variant, timeControl match {
      case TimeControl.Correspondence(d) => d.some
      case _                             => none
    }) | PerfType.Correspondence

  val idSize = 8

  private def randomId = ornicar.scalalib.Random nextStringUppercase idSize

  def make(
    variant: Variant,
    timeControl: TimeControl,
    mode: Mode,
    color: String,
    challenger: Option[lila.user.User],
    destUserId: Option[String]): Challenge = new Challenge(
    _id = randomId,
    variant = variant,
    timeControl = timeControl,
    mode = mode,
    color = color match {
      case "white" => ColorChoice.White
      case "black" => ColorChoice.Black
      case _       => ColorChoice.Random
    },
    challenger = challenger.fold[EitherChallenger](Left(Anonymous(randomId))) { u =>
      Right(Registered(u.id, Rating(u.perfs(perfType(variant, timeControl)))))
    },
    destUserId = destUserId,
    createdAt = DateTime.now,
    expiresAt = DateTime.now plusDays challenger.isDefined.fold(7, 1))
}

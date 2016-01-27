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
    challenger: Option[Challenge.Challenger],
    destUserId: Option[String],
    createdAt: DateTime) {

  def id = _id

  lazy val perfType = Challenge.perfType(variant, timeControl)
}

object Challenge {

  private[challenge] case class Challenger(id: String, name: String, rating: Int)

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

  def make(
    variant: Variant,
    timeControl: TimeControl,
    rated: Boolean,
    color: String,
    user: Option[lila.user.User],
    destUserId: Option[String]): Challenge = new Challenge(
    _id = ornicar.scalalib.Random nextStringUppercase idSize,
    variant = variant,
    timeControl = timeControl,
    mode = Mode(rated),
    color = color match {
      case "white" => ColorChoice.White
      case "black" => ColorChoice.Black
      case _       => ColorChoice.Random
    },
    challenger = user.map { u =>
      Challenger(
        id = u.id,
        name = u.username,
        rating = u.perfs(perfType(variant, timeControl)).intRating)
    },
    destUserId = destUserId,
    createdAt = DateTime.now)
}

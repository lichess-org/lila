package lila.tournament

import play.api.data.Forms._
import play.api.i18n.Lang

import lila.common.Form._

sealed trait TimeControl {
  def clock: Option[shogi.Clock.Config]
  def days: Option[Int]
  def estimateTotalSeconds: Int
  def show(implicit lang: Lang): String
}

object TimeControl {

  case class RealTime(value: shogi.Clock.Config) extends TimeControl {
    def clock                     = value.some
    def days                      = none
    def estimateTotalSeconds      = value.estimateTotalSeconds
    def show(implicit lang: Lang) = value.show
  }
  object RealTime {
    val id = 1
  }

  case class Correspondence(value: Int) extends TimeControl {
    def clock                = none
    def days                 = value.some
    def estimateTotalSeconds = value * 24 * 60 * 60
    def show(implicit lang: Lang) =
      s"${lila.i18n.I18nKeys.daysPerTurn}: $value"
  }
  object Correspondence {
    val id = 2
  }

  object DataForm {
    val timeControls = Seq(
      RealTime.id,
      Correspondence.id
    )
    val timeControlDefault = RealTime.id

    val clockTimes: Seq[Double] = Seq(0d, 1 / 4d, 1 / 2d, 3 / 4d, 1d, 3 / 2d) ++ {
      (2 to 7 by 1) ++ (10 to 30 by 5) ++ (40 to 60 by 10)
    }.map(_.toDouble)
    val clockTimeDefault = 10d

    val clockIncrements       = (0 to 2 by 1) ++ (3 to 7) ++ (10 to 30 by 5) ++ (40 to 60 by 10)
    val clockIncrementDefault = 0

    val clockByoyomi        = (0 to 2 by 1) ++ (3 to 7) ++ (10 to 30 by 5) ++ (40 to 60 by 10)
    val clockByoyomiDefault = 0

    val periods        = 1 to 5
    val periodsDefault = 1

    val clockConfigDefault = shogi.Clock.Config(
      (clockTimeDefault * 60).toInt,
      clockIncrementDefault,
      clockByoyomiDefault,
      periodsDefault
    )

    val daysPerTurn        = 1 to 5
    val daysPerTurnDefault = 1

    val setup = mapping(
      "timeControl"    -> numberIn(timeControls),
      "clockTime"      -> numberInDouble(clockTimes),
      "clockIncrement" -> numberIn(clockIncrements),
      "clockByoyomi"   -> numberIn(clockByoyomi),
      "periods"        -> numberIn(periods),
      "daysPerTurn"    -> numberIn(daysPerTurn)
    )(Setup.apply)(Setup.unapply)
      .verifying("Invalid clock setup", _.validClock)

    case class Setup(
        timeControl: Int,
        clockTime: Double,
        clockIncrement: Int,
        clockByoyomi: Int,
        periods: Int,
        daysPerTurn: Int
    ) {

      def isCorrespondence = timeControl == Correspondence.id
      def isRealTime       = timeControl == RealTime.id

      def convert: TimeControl =
        if (isCorrespondence)
          Correspondence(daysPerTurn)
        else RealTime(shogi.Clock.Config((clockTime * 60).toInt, clockIncrement, clockByoyomi, periods))

      def clock = convert.clock
      def days  = convert.days

      def validClock =
        if (isCorrespondence)
          daysPerTurn > 0
        else
          ((clockTime + clockIncrement) > 0 || (clockTime + clockByoyomi) > 0) && periods >= 1

    }
    object Setup {

      val default = Setup(
        timeControl = timeControlDefault,
        clockTime = clockTimeDefault,
        clockIncrement = clockIncrementDefault,
        clockByoyomi = clockByoyomiDefault,
        periods = periodsDefault,
        daysPerTurn = daysPerTurnDefault
      )

      def apply(tc: TimeControl): Setup =
        Setup(
          timeControl = if (tc.clock.isDefined) RealTime.id else Correspondence.id,
          clockTime = tc.clock.map(_.limitInMinutes) | clockTimeDefault,
          clockIncrement = tc.clock.map(_.incrementSeconds) | clockIncrementDefault,
          clockByoyomi = tc.clock.map(_.byoyomiSeconds) | clockByoyomiDefault,
          periods = tc.clock.map(_.periodsTotal) | periodsDefault,
          daysPerTurn = tc.days | daysPerTurnDefault
        )
    }
  }
}

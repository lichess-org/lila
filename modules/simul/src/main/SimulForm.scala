package lila.simul

import play.api.data._
import play.api.data.Forms._

import lila.common.Form._
import lila.hub.lightTeam._

object SimulForm {

  val clockTimes = (5 to 15 by 5) ++ (20 to 90 by 10) ++ (120 to 180 by 20)
  val clockTimeDefault = 20
  val clockTimeChoices = options(clockTimes, "%d minute{s}")

  val clockIncrements = (0 to 2 by 1) ++ (3 to 7) ++ (10 to 30 by 5) ++ (40 to 60 by 10) ++ (90 to 180 by 30)
  val clockIncrementDefault = 60
  val clockIncrementChoices = options(clockIncrements, "%d second{s}")

  val clockExtras = (0 to 15 by 5) ++ (20 to 60 by 10) ++ (90 to 120 by 30)
  val clockExtraChoices = options(clockExtras, "%d minute{s}")
  val clockExtraDefault = 0

  val colors = List("white", "random", "black")
  val colorChoices = List(
    "white" -> "White",
    "random" -> "Random",
    "black" -> "Black"
  )
  val colorDefault = "white"

  def create = Form(mapping(
    "clockTime" -> numberIn(clockTimeChoices),
    "clockIncrement" -> numberIn(clockIncrementChoices),
    "clockExtra" -> numberIn(clockExtraChoices),
    "variants" -> list {
      number.verifying(Set(chess.variant.Standard.id, chess.variant.Chess960.id,
        chess.variant.KingOfTheHill.id, chess.variant.ThreeCheck.id,
        chess.variant.Antichess.id, chess.variant.Atomic.id, chess.variant.Horde.id, chess.variant.RacingKings.id, chess.variant.Crazyhouse.id) contains _)
    }.verifying("At least one variant", _.nonEmpty),
    "color" -> stringIn(colorChoices),
    "text" -> text,
    "team" -> optional(nonEmptyText)
  )(Setup.apply)(Setup.unapply)) fill Setup(
    clockTime = clockTimeDefault,
    clockIncrement = clockIncrementDefault,
    clockExtra = clockExtraDefault,
    variants = List(chess.variant.Standard.id),
    color = colorDefault,
    text = "",
    team = none
  )

  def setText = Form(single("text" -> text))

  case class Setup(
      clockTime: Int,
      clockIncrement: Int,
      clockExtra: Int,
      variants: List[Int],
      color: String,
      text: String,
      team: Option[String]
  )
}

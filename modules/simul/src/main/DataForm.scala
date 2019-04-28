package lidraughts.simul

import play.api.data._
import play.api.data.Forms._

import lidraughts.common.Form._

final class DataForm {

  import DataForm._

  def create = Form(mapping(
    "clockTime" -> numberIn(clockTimeChoices),
    "clockIncrement" -> numberIn(clockIncrementChoices),
    "clockExtra" -> numberIn(clockExtraChoices),
    "variants" -> list {
      number.verifying(Set(draughts.variant.Standard.id, draughts.variant.Frisian.id, draughts.variant.Frysk.id, draughts.variant.Antidraughts.id, draughts.variant.Breakthrough.id) contains _)
    }.verifying("atLeastOneVariant", _.nonEmpty),
    "color" -> stringIn(colorChoices),
    "chat" -> stringIn(chatChoices),
    "targetPct" -> text(minLength = 0, maxLength = 3)
      .verifying("invalidTargetPercentage", pct => pct.length == 0 || parseIntOption(pct).fold(false)(p => p >= 50 && p <= 100)),
      "text"-> text
  )(SimulSetup.apply)(SimulSetup.unapply)) fill empty

  lazy val applyVariants = Form(mapping(
    "variants" -> list {
      number.verifying(Set(draughts.variant.Standard.id, draughts.variant.Frisian.id, draughts.variant.Frysk.id, draughts.variant.Antidraughts.id, draughts.variant.Breakthrough.id) contains _)
    }
  )(VariantsData.apply)(VariantsData.unapply)) fill VariantsData(
    variants = List(draughts.variant.Standard.id)
  )
}

object DataForm {

  case class VariantsData(
      variants: List[Int]
  )

  val clockTimes = (5 to 45 by 5) ++ (50 to 120 by 10) ++ (140 to 180 by 20)
  val clockTimeDefault = 20
  val clockTimeChoices = options(clockTimes, "%d minute{s}")

  val clockIncrements = (0 to 8) ++ (10 to 30 by 5) ++ (40 to 60 by 10) ++ (90 to 180 by 30)
  val clockIncrementDefault = 60
  val clockIncrementChoices = options(clockIncrements, "%d second{s}")

  val clockExtras = (0 to 15 by 5) ++ (20 to 60 by 10) ++ (90 to 120 by 30)
  val clockExtraChoices = options(clockExtras, "%d minute{s}")
  val clockExtraDefault = 0

  val colorChoices = List(
    "white" -> "White",
    "random" -> "Random",
    "black" -> "Black"
  )
  val colorDefault = "white"

  val chatChoices = List(
    "everyone" -> "Everyone",
    "spectators" -> "Spectators only",
    "participants" -> "Participants only"
  )
  val chatDefault = "everyone"

  val empty = SimulSetup(
    clockTime = clockTimeDefault,
    clockIncrement = clockIncrementDefault,
    clockExtra = clockExtraDefault,
    variants = List(draughts.variant.Standard.id),
    color = colorDefault,
    chat = chatDefault,
    targetPct = zero[String],
    text = ""
  )
}

case class SimulSetup(
    clockTime: Int,
    clockIncrement: Int,
    clockExtra: Int,
    variants: List[Int],
    color: String,
    chat: String,
    targetPct: String,
    text: String
)

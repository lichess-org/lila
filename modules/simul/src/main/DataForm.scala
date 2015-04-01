package lila.simul

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import chess.Mode
import lila.common.Form._

final class DataForm {

  val clockTimes = (5 to 15 by 5) ++ (20 to 90 by 10) ++ (120 to 240 by 30)
  val clockTimeDefault = 20
  val clockTimeChoices = options(clockTimes, "%d minute{s}")

  val clockIncrements = (0 to 2 by 1) ++ (3 to 7) ++ (10 to 30 by 5) ++ (40 to 60 by 10) ++ (90 to 180 by 30)
  val clockIncrementDefault = 60
  val clockIncrementChoices = options(clockIncrements, "%d second{s}")

  val clockMultipliers = 1 to 5
  val clockMultiplierChoices = clockMultipliers map {
    case 1 => (1, "Same as participants")
    case 2 => (2, "Twice more time")
    case x => (x, s"${x}x more time")
  }
  val clockMultiplierDefault = 1

  def create(defaultName: String) = Form(mapping(
    "name" -> text(minLength = 4),
    "clockTime" -> numberIn(clockTimeChoices),
    "clockIncrement" -> numberIn(clockIncrementChoices),
    "clockMultiplier" -> numberIn(clockMultiplierChoices),
    "variants" -> list {
      number.verifying(Set(chess.variant.Standard.id, chess.variant.Chess960.id, chess.variant.KingOfTheHill.id,
        chess.variant.ThreeCheck.id, chess.variant.Antichess.id, chess.variant.Atomic.id, chess.variant.Horde.id) contains _)
    }.verifying("At least one variant", _.nonEmpty)
  )(SimulSetup.apply)(SimulSetup.unapply)
  ) fill SimulSetup(
    name = defaultName,
    clockTime = clockTimeDefault,
    clockIncrement = clockIncrementDefault,
    clockMultiplier = clockMultiplierDefault,
    variants = List(chess.variant.Standard.id))
}

case class SimulSetup(
  name: String,
  clockTime: Int,
  clockIncrement: Int,
  clockMultiplier: Int,
  variants: List[Int])

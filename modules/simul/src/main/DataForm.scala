package lila.simul

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import chess.Mode
import lila.common.Form._
import lila.common.LameName

final class DataForm {

  val clockTimes = (5 to 15 by 5) ++ (20 to 90 by 10) ++ (120 to 240 by 30)
  val clockTimeDefault = 20
  val clockTimeChoices = options(clockTimes, "%d minute{s}")

  val clockIncrements = (0 to 2 by 1) ++ (3 to 7) ++ (10 to 30 by 5) ++ (40 to 60 by 10) ++ (90 to 180 by 30)
  val clockIncrementDefault = 60
  val clockIncrementChoices = options(clockIncrements, "%d second{s}")

  val clockExtras = (10 to 60 by 10) ++ (90 to 120 by 30)
  val clockExtraChoices = options(clockExtras, "%d minute{s}")
  val clockExtraDefault = 30

  def create(defaultName: String) = Form(mapping(
    "name" -> text(minLength = 4)
      .verifying("This name is not acceptable", n => !LameName(n)),
    "clockTime" -> numberIn(clockTimeChoices),
    "clockIncrement" -> numberIn(clockIncrementChoices),
    "clockExtra" -> numberIn(clockExtraChoices),
    "variants" -> list {
      number.verifying(Set(chess.variant.Standard.id, chess.variant.Chess960.id, chess.variant.KingOfTheHill.id,
        chess.variant.ThreeCheck.id, chess.variant.Antichess.id, chess.variant.Atomic.id, chess.variant.Horde.id) contains _)
    }.verifying("At least one variant", _.nonEmpty)
  )(SimulSetup.apply)(SimulSetup.unapply)
  ) fill SimulSetup(
    name = defaultName,
    clockTime = clockTimeDefault,
    clockIncrement = clockIncrementDefault,
    clockExtra = clockExtraDefault,
    variants = List(chess.variant.Standard.id))
}

case class SimulSetup(
  name: String,
  clockTime: Int,
  clockIncrement: Int,
  clockExtra: Int,
  variants: List[Int])

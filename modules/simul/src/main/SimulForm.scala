package lila.simul

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.{ Constraint, Constraints }
import lila.user.User

import chess.StartingPosition
import lila.common.Form._

object SimulForm {

  val clockTimes       = (5 to 15 by 5) ++ (20 to 90 by 10) ++ (120 to 180 by 20)
  val clockTimeDefault = 20
  val clockTimeChoices = options(clockTimes, "%d minute{s}")

  val clockIncrements       = (0 to 2 by 1) ++ (3 to 7) ++ (10 to 30 by 5) ++ (40 to 60 by 10) ++ (90 to 180 by 30)
  val clockIncrementDefault = 60
  val clockIncrementChoices = options(clockIncrements, "%d second{s}")

  val clockExtras       = (0 to 15 by 5) ++ (20 to 60 by 10) ++ (90 to 120 by 30)
  val clockExtraChoices = options(clockExtras, "%d minute{s}")
  val clockExtraDefault = 0

  val colors = List("white", "random", "black")
  val colorChoices = List(
    "white"  -> "White",
    "random" -> "Random",
    "black"  -> "Black"
  )
  val colorDefault = "white"

  private def nameType(host: User) =
    clean(text).verifying(
      Constraints minLength 2,
      Constraints maxLength 40,
      Constraints.pattern(
        regex = """[\p{L}\p{N}-\s:,;]+""".r,
        error = "error.unknown"
      ),
      Constraint[String] { (t: String) =>
        if (t.toLowerCase contains "lichess")
          validation.Invalid(validation.ValidationError("Must not contain \"lichess\""))
        else validation.Valid
      },
      Constraint[String] { (t: String) =>
        if (
          t.toUpperCase.split(' ').exists { word =>
            lila.user.Title.all.exists {
              case (title, name) =>
                !host.title.has(title) && {
                  title.value == word || name.toUpperCase == word
                }
            }
          }
        )
          validation.Invalid(validation.ValidationError("Must not contain a title"))
        else validation.Valid
      }
    )

  def create(host: User) =
    Form(
      mapping(
        "name"           -> nameType(host),
        "clockTime"      -> numberIn(clockTimeChoices),
        "clockIncrement" -> numberIn(clockIncrementChoices),
        "clockExtra"     -> numberIn(clockExtraChoices),
        "variants" -> list {
          number.verifying(
            Set(
              chess.variant.Standard.id,
              chess.variant.Chess960.id,
              chess.variant.KingOfTheHill.id,
              chess.variant.ThreeCheck.id,
              chess.variant.Antichess.id,
              chess.variant.Atomic.id,
              chess.variant.Horde.id,
              chess.variant.RacingKings.id,
              chess.variant.Crazyhouse.id
            ) contains _
          )
        }.verifying("At least one variant", _.nonEmpty),
        "position" -> optional(nonEmptyText),
        "color"    -> stringIn(colorChoices),
        "text"     -> clean(text),
        "team"     -> optional(nonEmptyText)
      )(Setup.apply)(Setup.unapply)
    ) fill Setup(
      name = host.titleUsername,
      clockTime = clockTimeDefault,
      clockIncrement = clockIncrementDefault,
      clockExtra = clockExtraDefault,
      variants = List(chess.variant.Standard.id),
      position = StartingPosition.initial.fen.some,
      color = colorDefault,
      text = "",
      team = none
    )

  val positions = StartingPosition.allWithInitial.map(_.fen)
  val positionChoices = StartingPosition.allWithInitial.map { p =>
    p.fen -> p.fullName
  }
  val positionDefault = StartingPosition.initial.fen

  def startingPosition(fen: String, variant: chess.variant.Variant): StartingPosition =
    Simul.fenIndex.get(fen).ifTrue(variant.standard) | StartingPosition.initial

  def setText = Form(single("text" -> text))

  case class Setup(
      name: String,
      clockTime: Int,
      clockIncrement: Int,
      clockExtra: Int,
      variants: List[Int],
      position: Option[String],
      color: String,
      text: String,
      team: Option[String]
  )
}

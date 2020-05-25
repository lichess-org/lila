package lidraughts.simul

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.{ Constraint, Constraints }

import lidraughts.common.Form._
import lidraughts.hub.lightTeam._
import lidraughts.user.User

object SimulForm {

  def canPickName(u: User) = {
    u.count.game >= 10 && u.createdSinceDays(3) && !u.troll
  } || u.hasTitle || u.isVerified

  private def nameType(host: User) = text.verifying(
    Constraints minLength 2,
    Constraints maxLength 40,
    Constraints.pattern(
      regex = """[\p{L}\p{N}-\s:,;]+""".r,
      error = "error.unknown"
    ),
    Constraint[String] { (t: String) =>
      if (t.toLowerCase contains "lidraughts")
        validation.Invalid(validation.ValidationError("Must not contain \"lidraughts\""))
      else validation.Valid
    },
    Constraint[String] { (t: String) =>
      if (t.toUpperCase.split(' ').exists { word =>
        lidraughts.user.Title.all.exists {
          case (title, name) =>
            !host.title.has(title) && {
              title.value == word || name.toUpperCase == word
            }
        }
      })
        validation.Invalid(validation.ValidationError("Must not contain a title"))
      else validation.Valid
    }
  )

  def create(host: User) = Form(mapping(
    "name" -> optional(nameType(host)),
    "clockTime" -> numberIn(clockTimeChoices),
    "clockIncrement" -> numberIn(clockIncrementChoices),
    "clockExtra" -> numberIn(clockExtraChoices),
    "variants" -> list {
      number.verifying(Set(draughts.variant.Standard.id, draughts.variant.Frisian.id, draughts.variant.Frysk.id, draughts.variant.Antidraughts.id, draughts.variant.Breakthrough.id, draughts.variant.Russian.id) contains _)
    }.verifying("atLeastOneVariant", _.nonEmpty),
    "color" -> stringIn(colorChoices),
    "targetPct" -> text(minLength = 0, maxLength = 3)
      .verifying("invalidTargetPercentage", pct => pct.length == 0 || parseIntOption(pct).fold(false)(p => p >= 50 && p <= 100)),
    "text" -> text,
    "team" -> optional(nonEmptyText)
  )(Setup.apply)(Setup.unapply)) fill empty(host)

  lazy val applyVariants = Form(mapping(
    "variants" -> list {
      number.verifying(Set(draughts.variant.Standard.id, draughts.variant.Frisian.id, draughts.variant.Frysk.id, draughts.variant.Antidraughts.id, draughts.variant.Breakthrough.id, draughts.variant.Russian.id) contains _)
    }
  )(VariantsData.apply)(VariantsData.unapply)) fill VariantsData(
    variants = List(draughts.variant.Standard.id)
  )

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

  def empty(host: User) = Setup(
    name = canPickName(host) option host.titleUsername,
    clockTime = clockTimeDefault,
    clockIncrement = clockIncrementDefault,
    clockExtra = clockExtraDefault,
    variants = List(draughts.variant.Standard.id),
    color = colorDefault,
    targetPct = zero[String],
    text = "",
    team = none
  )

  def setText = Form(single("text" -> text))

  case class Setup(
      name: Option[String],
      clockTime: Int,
      clockIncrement: Int,
      clockExtra: Int,
      variants: List[Int],
      color: String,
      targetPct: String,
      text: String,
      team: Option[String]
  )
}
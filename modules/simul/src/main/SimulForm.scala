package lila.simul

import cats.implicits._
import chess.format.FEN
import chess.StartingPosition
import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraint

import lila.common.Form._
import lila.hub.LeaderTeam
import lila.user.User

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
    eventName(2, 40, host.isVerifiedOrAdmin).verifying(
      Constraint[String] { (t: String) =>
        if (
          t.toUpperCase.split(' ').exists { word =>
            lila.user.Title.all.exists { case (title, name) =>
              !host.title.has(title) && (title.value == word || name.toUpperCase == word)
            }
          }
        )
          validation.Invalid(validation.ValidationError("Must not contain a title"))
        else validation.Valid
      }
    )

  def create(host: User, teams: List[LeaderTeam]) =
    baseForm(host, teams) fill Setup(
      name = host.titleUsername,
      clockTime = clockTimeDefault,
      clockIncrement = clockIncrementDefault,
      clockExtra = clockExtraDefault,
      variants = List(chess.variant.Standard.id),
      position = none,
      color = colorDefault,
      text = "",
      estimatedStartAt = none,
      team = none,
      featured = host.hasTitle.some
    )

  def edit(host: User, teams: List[LeaderTeam], simul: Simul) =
    baseForm(host, teams) fill Setup(
      name = simul.name,
      clockTime = simul.clock.config.limitInMinutes.toInt,
      clockIncrement = simul.clock.config.increment.roundSeconds,
      clockExtra = simul.clock.hostExtraMinutes,
      variants = simul.variants.map(_.id),
      position = simul.position,
      color = simul.color | "random",
      text = simul.text,
      estimatedStartAt = simul.estimatedStartAt,
      team = simul.team,
      featured = simul.featurable
    )

  private def baseForm(host: User, teams: List[LeaderTeam]) =
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
        "position"         -> optional(lila.common.Form.fen.playableStrict),
        "color"            -> stringIn(colorChoices),
        "text"             -> cleanText,
        "estimatedStartAt" -> optional(inTheFuture(ISODateTimeOrTimestamp.isoDateTimeOrTimestamp)),
        "team"             -> optional(nonEmptyText.verifying(id => teams.exists(_.id == id))),
        "featured"         -> optional(boolean)
      )(Setup.apply)(Setup.unapply)
    )

  val positions = StartingPosition.allWithInitial.map(_.fen)
  val positionChoices = StartingPosition.allWithInitial.map { p =>
    p.fen -> p.fullName
  }
  val positionDefault = StartingPosition.initial.fen

  def setText = Form(single("text" -> text))

  case class Setup(
      name: String,
      clockTime: Int,
      clockIncrement: Int,
      clockExtra: Int,
      variants: List[Int],
      position: Option[FEN],
      color: String,
      text: String,
      estimatedStartAt: Option[DateTime] = None,
      team: Option[String],
      featured: Option[Boolean]
  ) {
    def clock =
      SimulClock(
        config = chess.Clock.Config(clockTime * 60, clockIncrement),
        hostExtraTime = clockExtra * 60
      )

    def actualVariants = variants.flatMap { chess.variant.Variant(_) }

    def realPosition = position.filterNot(_.initial)
  }
}

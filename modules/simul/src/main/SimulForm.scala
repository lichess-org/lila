package lila.simul

import cats.implicits.*
import chess.format.Fen
import chess.StartingPosition
import org.joda.time.DateTime
import play.api.data.*
import play.api.data.Forms.*
import play.api.data.validation.Constraint

import lila.common.Form.{ *, given }
import lila.hub.LeaderTeam
import lila.user.User

object SimulForm:

  val clockTimes       = (5 to 15 by 5) ++ (20 to 90 by 10) ++ (120 to 180 by 20)
  val clockTimeDefault = 20
  val clockTimeChoices = options(clockTimes, "%d minute{s}")

  val clockIncrements = (0 to 2 by 1) ++ (3 to 7) ++ (10 to 30 by 5) ++ (40 to 60 by 10) ++ (90 to 180 by 30)
  val clockIncrementDefault = 60
  val clockIncrementChoices = options(clockIncrements, "%d second{s}")

  val clockExtrasPositive = (0 to 15 by 5) ++ (20 to 60 by 10) ++ (90 to 120 by 30)
  val clockExtras         = clockExtrasPositive.tail.map(-_).reverse concat clockExtrasPositive
  val clockExtraChoices = options(clockExtras, "%d minute{s}") map {
    case (d, str) if d > 0 => (d, s"+$str")
    case pair              => pair
  }
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
            chess.variant.Variant.all.filterNot(chess.variant.FromPosition ==).map(_.id).contains
          )
        }.verifying("At least one variant", _.nonEmpty),
        "position"         -> optional(lila.common.Form.fen.playableStrict),
        "color"            -> stringIn(colorChoices),
        "text"             -> cleanText,
        "estimatedStartAt" -> optional(inTheFuture(ISODateTimeOrTimestamp.isoDateTimeOrTimestamp)),
        "team"             -> optional(of[TeamId].verifying(id => teams.exists(_.id == id))),
        "featured"         -> optional(boolean)
      )(Setup.apply)(unapply)
        .verifying("Invalid host extra time.", _.clock.valid)
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
      position: Option[Fen],
      color: String,
      text: String,
      estimatedStartAt: Option[DateTime] = None,
      team: Option[TeamId],
      featured: Option[Boolean]
  ):

    def clock =
      SimulClock(
        config = chess.Clock.Config(clockTime * 60, clockIncrement),
        hostExtraTime = clockExtra * 60
      )

    def actualVariants = variants.flatMap { chess.variant.Variant(_) }

    def realPosition = position.filterNot(_.isInitial)

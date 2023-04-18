package lila.simul

import cats.syntax.all.*
import chess.format.Fen
import chess.variant.Variant
import play.api.data.*
import play.api.data.Forms.*
import play.api.data.validation.Constraint

import lila.common.Form.{ *, given }
import lila.hub.LeaderTeam
import lila.user.User
import chess.Clock
import chess.Clock.{ LimitMinutes, LimitSeconds, IncrementSeconds }

object SimulForm:

  val clockTimeChoices = options((5 to 15 by 5) ++ (20 to 90 by 10) ++ (120 to 180 by 20), "%d minute{s}")
  val clockTimeDefault = LimitMinutes(20)

  val clockIncrementChoices = options(
    (0 to 2 by 1) ++ (3 to 7) ++ (10 to 30 by 5) ++ (40 to 60 by 10) ++ (90 to 180 by 30),
    "%d second{s}"
  )
  val clockIncrementDefault = IncrementSeconds(60)

  val clockExtrasPositive = (0 to 15 by 5) ++ (20 to 60 by 10) ++ (90 to 120 by 30)
  val clockExtras         = clockExtrasPositive.tail.map(-_).reverse concat clockExtrasPositive
  val clockExtraChoices = options(clockExtras, "%d minute{s}") map {
    case (d, str) if d > 0 => (d, s"+$str")
    case pair              => pair
  }
  val clockExtraDefault = LimitMinutes(0)

  val clockExtraPerPlayerChoices = options((0 to 60 by 10) ++ Seq(90, 120, 180, 240, 300), "%d second{s}")
  val clockExtraPerPlayerDefault = LimitSeconds(0)

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
      clockExtraPerPlayer = clockExtraPerPlayerDefault,
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
      clockTime = LimitMinutes(simul.clock.config.limitInMinutes.toInt),
      clockIncrement = simul.clock.config.incrementSeconds,
      clockExtra = simul.clock.hostExtraMinutes,
      clockExtraPerPlayer = simul.clock.hostExtraTimePerPlayer,
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
        "name"                -> nameType(host),
        "clockTime"           -> numberIn(clockTimeChoices).into[LimitMinutes],
        "clockIncrement"      -> numberIn(clockIncrementChoices).into[IncrementSeconds],
        "clockExtra"          -> numberIn(clockExtraChoices).into[LimitMinutes],
        "clockExtraPerPlayer" -> numberIn(clockExtraPerPlayerChoices).into[LimitSeconds],
        "variants" -> list {
          typeIn(Variant.list.all.filter(chess.variant.FromPosition != _).map(_.id).toSet)
        }.verifying("At least one variant", _.nonEmpty),
        "position"         -> optional(lila.common.Form.fen.playableStrict),
        "color"            -> stringIn(colorChoices),
        "text"             -> cleanText,
        "estimatedStartAt" -> optional(inTheFuture(ISOInstantOrTimestamp.mapping)),
        "team"             -> optional(of[TeamId].verifying(id => teams.exists(_.id == id))),
        "featured"         -> optional(boolean)
      )(Setup.apply)(unapply)
        .verifying("Invalid host extra time.", _.clock.valid)
    )

  def setText = Form(single("text" -> text))

  case class Setup(
      name: String,
      clockTime: LimitMinutes,
      clockIncrement: IncrementSeconds,
      clockExtra: LimitMinutes,
      clockExtraPerPlayer: LimitSeconds,
      variants: List[Variant.Id],
      position: Option[Fen.Epd],
      color: String,
      text: String,
      estimatedStartAt: Option[Instant] = None,
      team: Option[TeamId],
      featured: Option[Boolean]
  ):

    def clock =
      SimulClock(
        config = Clock.Config(LimitSeconds(clockTime.value * 60), clockIncrement),
        hostExtraTime = LimitSeconds(clockExtra.value * 60),
        hostExtraTimePerPlayer = clockExtraPerPlayer
      )

    def actualVariants = variants.flatMap { Variant(_) }

    def realPosition = position.filterNot(_.isInitial)

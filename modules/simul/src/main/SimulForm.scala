package lila.simul

import chess.Clock
import chess.Clock.{IncrementSeconds, LimitMinutes, LimitSeconds}
import chess.format.Fen
import chess.variant.Variant
import play.api.data.*
import play.api.data.Forms.*
import play.api.data.validation.Constraint

import lila.common.Form.{ *, given }
import lila.core.i18n.Translate
import lila.core.team.LightTeam

object SimulForm:

  private given Translate = Translate

  private def colorChoice(color: String): (String, String) =
    color -> (color match
      case "white" => trans.site.white.txt()
      case "black" => trans.site.black.txt()
      case _       => trans.site.random.txt()
    )

  private def clockMinute(d: Int): String = trans.site.clockMinute.pluralTxt(d)
  private def clockSecond(d: Int): String = trans.site.clockSecond.pluralTxt(d)
  private def clockExtraMinute(d: Int): String = trans.site.clockExtraMinute.pluralTxt(d)
  private def clockExtraPerPlayerSecond(d: Int): String =
    trans.site.clockExtraPerPlayerSecond.pluralTxt(d)

  val colors = List("white", "random", "black")
  val colorChoices = colors.map(colorChoice)
  val colorDefault = "white"

  val clockTimeChoices =
    (5 to 15 by 5).map(d => d -> clockMinute(d)) ++
    (20 to 90 by 10).map(d => d -> clockMinute(d)) ++
    (120 to 180 by 20).map(d => d -> clockMinute(d))
  val clockTimeDefault = LimitMinutes(20)

  val clockIncrementChoices =
    (0 to 2 by 1).map(d => d -> clockSecond(d)) ++
    (3 to 7).map(d => d -> clockSecond(d)) ++
    (10 to 30 by 5).map(d => d -> clockSecond(d)) ++
    (40 to 60 by 10).map(d => d -> clockSecond(d)) ++
    (90 to 180 by 30).map(d => d -> clockSecond(d))
  val clockIncrementDefault = IncrementSeconds(60)

  val clockExtrasPositive = (0 to 15 by 5) ++ (20 to 60 by 10) ++ (90 to 120 by 30)
  val clockExtras = clockExtrasPositive.tail.map(-_).reverse ++ clockExtrasPositive
  val clockExtraChoices = clockExtras.map:
    case d if d > 0 => d -> s"+${clockExtraMinute(d)}"
    case d          => d -> clockExtraMinute(d)
  val clockExtraDefault = LimitMinutes(0)

  val clockExtraPerPlayerChoices =
    (0 to 60 by 10).map(d => d -> clockExtraPerPlayerSecond(d)) ++
    Seq(90, 120, 180, 240, 300).map(d => d -> clockExtraPerPlayerSecond(d))
  val clockExtraPerPlayerDefault = LimitSeconds(0)

  private def nameType(using host: Me) =
    eventName(2, 40, host.isVerifiedOrAdmin).verifying(
      Constraint[String]: (t: String) =>
        if t.toUpperCase.split(' ').exists { word =>
            chess.PlayerTitle.all.exists: (title, name) =>
              !host.title.contains(title) && (title.value == word || name.toUpperCase == word)
          }
        then validation.Invalid(validation.ValidationError("Must not contain a title"))
        else validation.Valid
    )

  def create(teams: List[LightTeam])(using host: Me) =
    baseForm(teams).fill(
      Setup(
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
        featured = host.hasTitle.some,
        conditions = SimulCondition.All.empty
      )
    )

  def edit(teams: List[LightTeam], simul: Simul)(using Me): EitherForm =
    if simul.isCreated
    then
      Right:
        baseForm(teams).fill(
          Setup(
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
            featured = simul.featurable,
            conditions = simul.conditions
          )
        )
    else Left(lockedForm.fill(LockedSetup(simul.name, simul.text)))

  private def baseForm(teams: List[LightTeam])(using host: Me) =
    Form(
      mapping(
        "name" -> nameType,
        "clockTime" -> numberIn(clockTimeChoices).into[LimitMinutes],
        "clockIncrement" -> numberIn(clockIncrementChoices).into[IncrementSeconds],
        "clockExtra" -> numberIn(clockExtraChoices).into[LimitMinutes],
        "clockExtraPerPlayer" -> numberIn(clockExtraPerPlayerChoices).into[LimitSeconds],
        "variants" -> list {
          typeIn(Variant.list.all.filter(chess.variant.FromPosition != _).map(_.id).toSet)
        }.verifying("At least one variant", _.nonEmpty),
        "position" -> optional(lila.common.Form.fen.playableStrict),
        "color" -> stringIn(colorChoices),
        "text" -> cleanText,
        "estimatedStartAt" -> optional(inTheFuture(ISOInstantOrTimestamp.mapping)),
        "featured" -> optional(boolean),
        "conditions" -> SimulCondition.form.all(teams)
      )(Setup.apply)(unapply)
        .verifying("Invalid host extra time.", _.clock.valid)
    )

  case class Setup(
      name: String,
      clockTime: LimitMinutes,
      clockIncrement: IncrementSeconds,
      clockExtra: LimitMinutes,
      clockExtraPerPlayer: LimitSeconds,
      variants: List[Variant.Id],
      position: Option[Fen.Full],
      color: String,
      text: String,
      estimatedStartAt: Option[Instant] = None,
      featured: Option[Boolean],
      conditions: SimulCondition.All
  ):
    def clock =
      SimulClock(
        config = Clock.Config(LimitSeconds(clockTime.value * 60), clockIncrement),
        hostExtraTime = LimitSeconds(clockExtra.value * 60),
        hostExtraTimePerPlayer = clockExtraPerPlayer
      )

    def actualVariants = variants.flatMap(Variant(_))

    def realPosition = position.filterNot(_.isInitial)

  def lockedForm(using Me) = Form:
    mapping(
      "name" -> nameType,
      "text" -> cleanText
    )(LockedSetup.apply)(unapply)

  case class LockedSetup(name: String, text: String)

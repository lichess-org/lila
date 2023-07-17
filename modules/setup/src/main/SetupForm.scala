package lila.setup

import chess.format.Fen
import chess.variant.Variant
import chess.Clock
import play.api.data.*
import play.api.data.Forms.*

import lila.rating.RatingRange
import lila.common.{ Days, Form as LilaForm }
import lila.common.Form.{ *, given }
import lila.user.Me

object SetupForm:

  import Mappings.*

  val filter = Form(single("local" -> text))

  def aiFilled(fen: Option[Fen.Epd]): Form[AiConfig] =
    ai fill fen.foldLeft(AiConfig.default): (config, f) =>
      config.copy(fen = f.some, variant = chess.variant.FromPosition)

  lazy val ai = Form:
    mapping(
      "variant"   -> aiVariants,
      "timeMode"  -> timeMode,
      "time"      -> time,
      "increment" -> increment,
      "days"      -> days,
      "level"     -> level,
      "color"     -> color,
      "fen"       -> fenField
    )(AiConfig.from)(_.>>)
      .verifying("invalidFen", _.validFen)
      .verifying("Can't play that time control from a position", _.timeControlFromPosition)

  def friendFilled(fen: Option[Fen.Epd])(using Option[Me]): Form[FriendConfig] =
    friend fill fen.foldLeft(FriendConfig.default): (config, f) =>
      config.copy(fen = f.some, variant = chess.variant.FromPosition)

  def friend(using me: Option[Me]) = Form:
    mapping(
      "variant"   -> variantWithFenAndVariants,
      "timeMode"  -> timeMode,
      "time"      -> time,
      "increment" -> increment,
      "days"      -> days,
      "mode"      -> mode(withRated = me.isDefined),
      "color"     -> color,
      "fen"       -> fenField
    )(FriendConfig.from)(_.>>)
      .verifying("Invalid clock", _.validClock)
      .verifying("Invalid speed", _.validSpeed(me.exists(_.isBot)))
      .verifying("invalidFen", _.validFen)

  def hookFilled(timeModeString: Option[String])(using me: Option[Me]): Form[HookConfig] =
    hook fill HookConfig.default(me.isDefined).withTimeModeString(timeModeString)

  def hook(using me: Option[Me]) = Form:
    mapping(
      "variant"     -> variantWithVariants,
      "timeMode"    -> timeMode,
      "time"        -> time,
      "increment"   -> increment,
      "days"        -> days,
      "mode"        -> mode(me.isDefined),
      "ratingRange" -> optional(ratingRange),
      "color"       -> color
    )(HookConfig.from)(_.>>)
      .verifying("Invalid clock", _.validClock)
      .verifying("Can't create rated unlimited in lobby", _.noRatedUnlimited)

  private lazy val boardApiHookBase: Mapping[HookConfig] =
    mapping(
      "time"        -> optional(time),
      "increment"   -> optional(increment),
      "days"        -> optional(days),
      "variant"     -> optional(boardApiVariantKeys),
      "rated"       -> optional(boolean),
      "color"       -> optional(color),
      "ratingRange" -> optional(ratingRange)
    )((t, i, d, v, r, c, g) =>
      HookConfig(
        variant = Variant.orDefault(v),
        timeMode = if d.isDefined then TimeMode.Correspondence else TimeMode.RealTime,
        time = t | 10,
        increment = i | Clock.IncrementSeconds(5),
        days = d | Days(7),
        mode = chess.Mode(~r),
        color = lila.lobby.Color.orDefault(c),
        ratingRange = g.fold(RatingRange.default)(RatingRange.orDefault)
      )
    )(_ => none)
      .verifying("Invalid clock", _.validClock)

  def boardApiHook(allowFastGames: Boolean) = Form:
    boardApiHookBase
      .verifying(
        "Invalid time control",
        hook =>
          allowFastGames || hook.makeClock.exists(
            lila.game.Game.isBoardCompatible
          ) || hook.makeDaysPerTurn.isDefined
      )

  object api:

    lazy val clockMapping =
      mapping(
        "limit"     -> number.into[Clock.LimitSeconds].verifying(ApiConfig.clockLimitSeconds.contains),
        "increment" -> increment
      )(Clock.Config.apply)(unapply)
        .verifying("Invalid clock", c => c.estimateTotalTime > chess.Centis(0))

    lazy val clock = "clock" -> optional(clockMapping)

    lazy val optionalDays = "days" -> optional(days)

    lazy val variant = "variant" -> optional(typeIn(Variant.list.all.map(_.key).toSet))

    lazy val message = optional(
      nonEmptyText(maxLength = 8_000).verifying(
        "The message must contain {game}, which will be replaced with the game URL.",
        _ contains "{game}"
      )
    )

    def user(using from: Me) =
      Form(challengeMapping.verifying("Invalid speed", _ validSpeed from.isBot))

    def admin = Form(challengeMapping)

    private val challengeMapping =
      mapping(
        variant,
        clock,
        optionalDays,
        "rated"           -> boolean,
        "color"           -> optional(color),
        "fen"             -> fenField,
        "message"         -> message,
        "keepAliveStream" -> optional(boolean),
        "rules"           -> optional(gameRules)
      )(ApiConfig.from)(_ => none)
        .verifying("invalidFen", _.validFen)
        .verifying("can't be rated", _.validRated)

    lazy val ai = Form:
      mapping(
        "level" -> level,
        variant,
        clock,
        optionalDays,
        "color" -> optional(color),
        "fen"   -> fenField
      )(ApiAiConfig.from)(_ => none).verifying("invalidFen", _.validFen)

    lazy val open = Form:
      mapping(
        "name" -> optional(LilaForm.cleanNonEmptyText(maxLength = 200)),
        variant,
        clock,
        optionalDays,
        "rated" -> boolean,
        "fen"   -> fenField,
        "users" -> optional:
          LilaForm.strings
            .separator(",")
            .verifying("Must be 2 usernames, white and black", _.sizeIs == 2)
            .transform[List[UserStr]](UserStr.from(_), UserStr.raw(_))
        ,
        "rules" -> optional(gameRules),
        "expiresAt" -> optional:
          inTheFuture(ISOInstantOrTimestamp.mapping)
            .verifying("Open challenges must expire within 2 weeks", _.isBefore(nowInstant.plusWeeks(2)))
      )(OpenConfig.from)(_ => none)
        .verifying("invalidFen", _.validFen)
        .verifying("rated without a clock", c => c.clock.isDefined || c.days.isDefined || !c.rated)

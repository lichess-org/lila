package lila.setup

import play.api.data._
import play.api.data.Forms._

import chess.format.FEN
import chess.variant.Variant
import lila.rating.RatingRange
import lila.user.UserContext

final class FormFactory {

  import Mappings._

  val filter = Form(single("local" -> text))

  def aiFilled(fen: Option[FEN]): Form[AiConfig] =
    ai fill fen.foldLeft(AiConfig.default) {
      case (config, f) => config.copy(fen = f.some, variant = chess.variant.FromPosition)
    }

  lazy val ai = Form(
    mapping(
      "variant"   -> aiVariants,
      "timeMode"  -> timeMode,
      "time"      -> time,
      "increment" -> increment,
      "byoyomi"   -> byoyomi,
      "periods"   -> periods,
      "days"      -> days,
      "level"     -> level,
      "color"     -> color,
      "fen"       -> fenField
    )(AiConfig.from)(_.>>)
      .verifying("invalidFen", _.validFen)
      .verifying("Can't play that time control from a position", _.timeControlFromPosition)
  )

  def friendFilled(fen: Option[FEN])(implicit ctx: UserContext): Form[FriendConfig] =
    friend(ctx) fill fen.foldLeft(FriendConfig.default) {
      case (config, f) => config.copy(fen = f.some, variant = chess.variant.FromPosition)
    }

  def friend(ctx: UserContext) =
    Form(
      mapping(
        "variant"   -> variantWithFenAndVariants,
        "timeMode"  -> timeMode,
        "time"      -> time,
        "increment" -> increment,
        "byoyomi"   -> byoyomi,
        "periods"   -> periods,
        "days"      -> days,
        "mode"      -> mode(withRated = ctx.isAuth),
        "color"     -> color,
        "fen"       -> fenField
      )(FriendConfig.from)(_.>>)
        .verifying("Invalid clock", _.validClock)
        .verifying("invalidFen", _.validFen)
    )

  def hookFilled(timeModeString: Option[String])(implicit ctx: UserContext): Form[HookConfig] =
    hook fill HookConfig.default.withTimeModeString(timeModeString)

  def hook(implicit ctx: UserContext) ={
    Form(
      mapping(
        "variant"     -> variantWithVariants,
        "timeMode"    -> timeMode,
        "time"        -> time,
        "increment"   -> increment,
        "byoyomi"     -> byoyomi,
        "periods"     -> periods,
        "days"        -> days,
        "mode"        -> mode(ctx.isAuth),
        "ratingRange" -> optional(ratingRange),
        "color"       -> color
      )(HookConfig.from)(_.>>)
        .verifying("Invalid clock", _.validClock)
        .verifying("Can't create rated unlimited in lobby", _.noRatedUnlimited)
    )
    }

  lazy val boardApiHook = Form(
    mapping(
      "time"        -> time,
      "increment"   -> increment,
      "byoyomi"     -> byoyomi,
      "periods"     -> periods,
      "variant"     -> optional(boardApiVariantKeys),
      "rated"       -> optional(boolean),
      "color"       -> optional(color),
      "ratingRange" -> optional(ratingRange)
    )((t, i, b, p, v, r, c, g) =>
      HookConfig(
        variant = v.flatMap(Variant.apply) | Variant.default,
        timeMode = TimeMode.RealTime,
        time = t,
        increment = i,
        byoyomi = b,
        periods = p,
        days = 1,
        mode = chess.Mode(~r),
        color = lila.lobby.Color.orDefault(c),
        ratingRange = g.fold(RatingRange.default)(RatingRange.orDefault)
      )
    )(_ => none)
      .verifying("Invalid clock", _.validClock)
      .verifying(
        "Invalid time control",
        hook =>
          hook.makeClock ?? {
            lila.game.Game.isBoardCompatible(_, hook.mode)
          }
      )
  )

  object api {

    private lazy val clock = "clock" -> optional(
      mapping(
        "limit"     -> number.verifying(ApiConfig.clockLimitSeconds.contains _),
        "increment" -> increment,
        "byoyomi" -> byoyomi,
        "periods" -> periods
      )(chess.Clock.Config.apply)(chess.Clock.Config.unapply)
    )

    private lazy val variant =
      "variant" -> optional(text.verifying(Variant.byKey.contains _))

    lazy val user = Form(
      mapping(
        variant,
        clock,
        "days"          -> optional(days),
        "rated"         -> boolean,
        "color"         -> optional(color),
        "fen"           -> fenField,
        "acceptByToken" -> optional(nonEmptyText)
      )(ApiConfig.from)(_.>>).verifying("invalidFen", _.validFen)
    )

    lazy val ai = Form(
      mapping(
        "level" -> level,
        variant,
        clock,
        "days"  -> optional(days),
        "color" -> optional(color),
        "fen"   -> fenField
      )(ApiAiConfig.from)(_.>>).verifying("invalidFen", _.validFen)
    )

    lazy val open = Form(
      mapping(
        variant,
        clock,
        "fen" -> fenField
      )(OpenConfig.from)(_.>>).verifying("invalidFen", _.validFen)
    )
  }
}

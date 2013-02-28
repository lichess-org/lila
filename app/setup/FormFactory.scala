package lila.app
package setup

import chess.Variant
import http.Context
import elo.EloRange

import play.api.data._
import play.api.data.Forms._
import scalaz.effects._

private[setup] final class FormFactory(
    userConfigRepo: UserConfigRepo,
    anonConfigRepo: AnonConfigRepo) {

  import Mappings._

  def filterFilled(implicit ctx: Context): IO[Form[FilterConfig]] =
    filterConfig map filter(ctx).fill

  def filter(ctx: Context) = Form(
    mapping(
      "variant" -> optional(variant),
      "mode" -> mode(ctx.isAuth),
      "speed" -> optional(speed),
      "eloDiff" -> optional(eloDiff)
    )(FilterConfig.<<)(_.>>)
  )

  def filterConfig(implicit ctx: Context): IO[FilterConfig] = savedConfig map { config ⇒
    ctx.isAuth.fold(config.filter, config.filter.withModeCasual)
  }

  def aiFilled(fen: Option[String])(implicit ctx: Context): IO[Form[AiConfig]] =
    aiConfig map { config ⇒
      ai(ctx) fill fen.fold(config) { f ⇒ 
        config.copy(fen = f.some, variant = Variant.FromPosition)
      }
    }

  def ai(ctx: Context) = Form(
    mapping(
      "variant" -> variantWithFen,
      "clock" -> clock,
      "time" -> time,
      "increment" -> increment,
      "level" -> level,
      "color" -> color,
      "fen" -> fen(true)
    )(AiConfig.<<)(_.>>)
  )

  def aiConfig(implicit ctx: Context): IO[AiConfig] = savedConfig map (_.ai)

  def friendFilled(fen: Option[String])(implicit ctx: Context): IO[Form[FriendConfig]] =
    friendConfig map { config ⇒
      friend(ctx) fill fen.fold(config) { f ⇒ 
        config.copy(fen = f.some, variant = Variant.FromPosition)
      }
    }

  def friend(ctx: Context) = Form(
    mapping(
      "variant" -> variantWithFen,
      "clock" -> clock,
      "time" -> time,
      "increment" -> increment,
      "mode" -> mode(ctx.isAuth),
      "color" -> color,
      "fen" -> fen(false)
    )(FriendConfig.<<)(_.>>) verifying ("Invalid clock", _.validClock)
  )

  def friendConfig(implicit ctx: Context): IO[FriendConfig] = savedConfig map (_.friend)

  def hookFilled(implicit ctx: Context): IO[Form[HookConfig]] =
    hookConfig map hook(ctx).fill

  def hook(ctx: Context) = Form(
    mapping(
      "variant" -> variant,
      "clock" -> clock,
      "time" -> time,
      "increment" -> increment,
      "mode" -> mode(ctx.isAuth),
      "eloRange" -> eloRange,
      "color" -> nonEmptyText.verifying(Color.names contains _)
    )(HookConfig.<<)(_.>>)
      .verifying("Invalid clock", _.validClock)
      .verifying("Can't create rated unlimited in lobby", _.noRatedUnlimited)
  )

  def hookConfig(implicit ctx: Context): IO[HookConfig] = savedConfig map (_.hook)

  private def savedConfig(implicit ctx: Context): IO[UserConfig] = 
    ctx.me.fold(anonConfigRepo config ctx.req)(userConfigRepo.config)
}

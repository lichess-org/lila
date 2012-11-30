package lila
package setup

import http.Context
import elo.EloRange

import play.api.data._
import play.api.data.Forms._
import scalaz.effects._

final class FormFactory(
    configRepo: UserConfigRepo) {

  import Mappings._

  def aiFilled(implicit ctx: Context): IO[Form[AiConfig]] =
    aiConfig map ai(ctx).fill

  def ai(ctx: Context) = Form(
    mapping(
      "variant" -> variant,
      "clock" -> clock,
      "time" -> time,
      "increment" -> increment,
      "level" -> level,
      "color" -> color
    )(AiConfig.<<)(_.>>)
  )

  def aiConfig(implicit ctx: Context): IO[AiConfig] =
    ctx.me.fold(io(AiConfig.default)) { user ⇒
      configRepo.config(user) map (_.ai)
    }

  def friendFilled(implicit ctx: Context): IO[Form[FriendConfig]] =
    friendConfig map friend(ctx).fill

  def friend(ctx: Context) = Form(
    mapping(
      "variant" -> variant,
      "clock" -> clock,
      "time" -> time,
      "increment" -> increment,
      "mode" -> mode(ctx.isAuth),
      "color" -> color
    )(FriendConfig.<<)(_.>>) verifying ("Invalid clock", _.validClock)
  )

  def friendConfig(implicit ctx: Context): IO[FriendConfig] =
    ctx.me.fold(io(FriendConfig.default)) { user ⇒
      configRepo.config(user) map (_.friend)
    }

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

  def hookConfig(implicit ctx: Context): IO[HookConfig] =
    ctx.me.fold(io(HookConfig.default)) { user ⇒
      configRepo.config(user) map (_.hook)
    }
}

package lila
package setup

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
      "mode" -> mode(true),
      "speed" -> optional(speed)
    )(FilterConfig.<<)(_.>>)
  )

  def filterConfig(implicit ctx: Context): IO[FilterConfig] = savedConfig map (_.filter)

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

  def aiConfig(implicit ctx: Context): IO[AiConfig] = savedConfig map (_.ai)

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

  private def savedConfig(implicit ctx: Context): IO[UserConfig] = ctx.me.fold(
    userConfigRepo.config,
    anonConfigRepo config ctx.req
  ) 
}

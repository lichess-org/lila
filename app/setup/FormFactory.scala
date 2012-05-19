package lila
package setup

import http.Context

import play.api.data._
import play.api.data.Forms._
import scalaz.effects._

final class FormFactory(
    configRepo: UserConfigRepo) {

  def aiFilled(implicit ctx: Context): IO[Form[AiConfig]] =
    aiConfig map ai.fill

  def ai = Form(
    mapping(
      "variant" -> number.verifying(Config.variants contains _),
      "level" -> number.verifying(AiConfig.levels contains _),
      "color" -> nonEmptyText.verifying(Color.names contains _)
    )(AiConfig.<<)(_.>>)
  )

  def aiConfig(implicit ctx: Context): IO[AiConfig] = ctx.me.fold(
    user ⇒ configRepo.config(user) map (_.ai),
    io(AiConfig.default)
  )

  def friendFilled(implicit ctx: Context): IO[Form[FriendConfig]] =
    friendConfig map friend.fill

  def friend = Form(
    mapping(
      "variant" -> number.verifying(Config.variants contains _),
      "clock" -> boolean,
      "time" -> number.verifying(FriendConfig.times contains _),
      "increment" -> number.verifying(FriendConfig.increments contains _),
      "mode" -> number.verifying(FriendConfig.modes contains _),
      "color" -> nonEmptyText.verifying(Color.names contains _)
    )(FriendConfig.<<)(_.>>) verifying ("Invalid clock", _.validClock)
  )

  def friendConfig(implicit ctx: Context): IO[FriendConfig] = ctx.me.fold(
    user ⇒ configRepo.config(user) map (_.friend),
    io(FriendConfig.default)
  )
}

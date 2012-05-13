package lila
package setup

import model.User

import com.novus.salat.annotations.Key

case class UserConfig(
  @Key("_id") id: String,
  ai: AiConfig
) {

  def withAi(c: AiConfig) = copy(ai = c)
}

object UserConfig {

  def default(user: User) = UserConfig(
    id = user.usernameCanonical,
    ai = AiConfig.default)
}

package lila
package setup

import user.User

import com.novus.salat.annotations.Key

case class UserConfig(
  id: String,
  ai: AiConfig
) {

  def withAi(c: AiConfig) = copy(ai = c)

  def encode = RawUserConfig(
    id = id,
    ai = ai.encode)
}

object UserConfig {

  def default(user: User) = UserConfig(
    id = user.usernameCanonical,
    ai = AiConfig.default)
}

case class RawUserConfig(
  @Key("_id") id: String,
  ai: RawAiConfig
) {

  def decode = for {
    trueAi <- ai.decode
  } yield UserConfig(
    id = id,
    ai = trueAi)
}

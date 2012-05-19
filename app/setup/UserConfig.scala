package lila
package setup

import user.User

import com.novus.salat.annotations.Key

case class UserConfig(
  id: String,
  ai: AiConfig,
  friend: FriendConfig
) {

  def withAi(c: AiConfig) = copy(ai = c)

  def withFriend(c: FriendConfig) = copy(friend = c)

  def encode = RawUserConfig(
    id = id,
    ai = ai.encode,
    friend = friend.encode)
}

object UserConfig {

  def default(user: User) = UserConfig(
    id = user.usernameCanonical,
    ai = AiConfig.default,
    friend = FriendConfig.default)
}

case class RawUserConfig(
  @Key("_id") id: String,
  ai: RawAiConfig,
  friend: RawFriendConfig
) {

  def decode = for {
    trueAi <- ai.decode
    trueFriend <- friend.decode
  } yield UserConfig(
    id = id,
    ai = trueAi,
    friend = trueFriend)
}

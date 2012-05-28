package lila
package setup

import user.User

import com.novus.salat.annotations.Key

case class UserConfig(
    id: String,
    ai: AiConfig,
    friend: FriendConfig,
    hook: HookConfig) {

  def withAi(c: AiConfig) = copy(ai = c)

  def withFriend(c: FriendConfig) = copy(friend = c)

  def withHook(c: HookConfig) = copy(hook = c)

  def encode = RawUserConfig(
    id = id,
    ai = ai.encode,
    friend = friend.encode,
    hook = hook.encode)
}

object UserConfig {

  def default(user: User) = UserConfig(
    id = user.id,
    ai = AiConfig.default,
    friend = FriendConfig.default,
    hook = HookConfig.default)
}

case class RawUserConfig(
    @Key("_id") id: String,
    ai: RawAiConfig,
    friend: RawFriendConfig,
    hook: RawHookConfig) {

  def decode = for {
    trueAi ← ai.decode
    trueFriend ← friend.decode
    trueHook ← hook.decode
  } yield UserConfig(
    id = id,
    ai = trueAi,
    friend = trueFriend,
    hook = trueHook)
}

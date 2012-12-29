package lila
package setup

import user.User
import ornicar.scalalib.Random

import org.joda.time.DateTime
import com.novus.salat.annotations.Key

case class UserConfig(
    id: String,
    ai: AiConfig,
    friend: FriendConfig,
    hook: HookConfig,
    filter: FilterConfig) {

  def withAi(c: AiConfig) = copy(ai = c)

  def withFriend(c: FriendConfig) = copy(friend = c)

  def withHook(c: HookConfig) = copy(hook = c)

  def encode = RawUserConfig(
    id = id,
    ai = ai.encode,
    friend = friend.encode,
    hook = hook.encode,
    filter = filter.encode.some,
    date = DateTime.now)
}

object UserConfig {

  def default(id: String): UserConfig = UserConfig(
    id = id,
    ai = AiConfig.default,
    friend = FriendConfig.default,
    hook = HookConfig.default,
    filter = FilterConfig.default)
}

case class RawUserConfig(
    @Key("_id") id: String,
    ai: RawAiConfig,
    friend: RawFriendConfig,
    hook: RawHookConfig,
    filter: Option[RawFilterConfig],
    date: DateTime) {

  def decode: Option[UserConfig] = for {
    trueAi ← ai.decode
    trueFriend ← friend.decode
    trueHook ← hook.decode
    trueFilter = filter.flatMap(_.decode) | FilterConfig.default
  } yield UserConfig(
    id = id,
    ai = trueAi,
    friend = trueFriend,
    hook = trueHook,
    filter = trueFilter)
}

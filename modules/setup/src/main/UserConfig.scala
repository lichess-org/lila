package lila.setup

import org.joda.time.DateTime

import lila.user.User

private[setup] case class UserConfig(
    id: String,
    ai: AiConfig,
    friend: FriendConfig,
    hook: HookConfig,
    filter: FilterConfig) {

  def withFilter(c: FilterConfig) = copy(filter = c)

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

private[setup] object UserConfig {

  def default(id: String): UserConfig = UserConfig(
    id = id,
    ai = AiConfig.default,
    friend = FriendConfig.default,
    hook = HookConfig.default,
    filter = FilterConfig.default)

  import lila.db.JsTube
  import play.api.libs.json._

  private[setup] lazy val tube = JsTube(
    reader = Reads[UserConfig](js =>
      ~(for {
        obj ← js.asOpt[JsObject]
        raw ← RawUserConfig.tube.read(obj).asOpt
        decoded ← raw.decode
      } yield JsSuccess(decoded): JsResult[UserConfig])
    ),
    writer = Writes[UserConfig](config =>
      RawUserConfig.tube.write(config.encode) getOrElse JsUndefined("[setup] Can't write config")
    )
  )
}

private[setup] case class RawUserConfig(
    id: String,
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

private[setup] object RawUserConfig {

  import lila.db.JsTube
  import JsTube.Helpers._
  import play.api.libs.json._

  private implicit def aiTube = RawAiConfig.tube
  private implicit def friendTube = RawFriendConfig.tube
  private implicit def hookTube = RawHookConfig.tube
  private implicit def filterTube = RawFilterConfig.tube

  private[setup] lazy val tube = JsTube(
    (__.json update readDate('date)) andThen Json.reads[RawUserConfig],
    Json.writes[RawUserConfig] andThen (__.json update writeDate('date))
  )
}

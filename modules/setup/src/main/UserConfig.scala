package lila.setup

private[setup] case class UserConfig(
    id: String,
    ai: AiConfig,
    friend: FriendConfig,
    hook: HookConfig,
    filter: FilterConfig
) {

  def withFilter(c: FilterConfig) = copy(filter = c)

  def withAi(c: AiConfig) = copy(ai = c)

  def withFriend(c: FriendConfig) = copy(friend = c)

  def withHook(c: HookConfig) = copy(hook = c)
}

private[setup] object UserConfig {

  def default(id: String): UserConfig = UserConfig(
    id = id,
    ai = AiConfig.default,
    friend = FriendConfig.default,
    hook = HookConfig.default,
    filter = FilterConfig.default
  )

  import lila.db.BSON
  import lila.db.dsl._
  import AiConfig.aiConfigBSONHandler
  import FriendConfig.friendConfigBSONHandler
  import HookConfig.hookConfigBSONHandler
  import FilterConfig.filterConfigBSONHandler

  private[setup] implicit val userConfigBSONHandler = new BSON[UserConfig] {

    def reads(r: BSON.Reader): UserConfig = UserConfig(
      id = r str "_id",
      ai = r.getO[AiConfig]("ai") | AiConfig.default,
      friend = r.getO[FriendConfig]("friend") | FriendConfig.default,
      hook = r.getO[HookConfig]("hook") | HookConfig.default,
      filter = r.getO[FilterConfig]("filter") | FilterConfig.default
    )

    def writes(w: BSON.Writer, o: UserConfig) = $doc(
      "_id" -> o.id,
      "ai" -> o.ai,
      "friend" -> o.friend,
      "hook" -> o.hook,
      "filter" -> o.filter
    )
  }
}

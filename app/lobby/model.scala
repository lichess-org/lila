package lila
package lobby

case class Member(
    channel: Channel,
    username: Option[String],
    hookOwnerId: Option[String]) {

  def ownsHook(hook: model.Hook) = Some(hook.ownerId) == hookOwnerId
}

case object GetHooks
case class Hooks(ownerIds: Iterable[String])
case object GetUsernames
case class Usernames(usernames: Set[String]) {
  def +(other: Usernames) = Usernames(usernames ++ other.usernames)
}
case object NbPlayers
case class AddHook(hook: model.Hook)
case class RemoveHook(hook: model.Hook)
case class BiteHook(hook: model.Hook, game: model.DbGame)
case class Entry(entry: model.Entry)
case class Join(
    uid: String,
    version: Int,
    username: Option[String],
    hookOwnerId: Option[String])
case class Quit(uid: String)
case class Talk(txt: String, u: String)
case class Connected(channel: Channel)

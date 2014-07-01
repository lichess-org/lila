package lila.pool
package actorApi

import lila.game.Game
import lila.socket.SocketMember
import lila.user.User

private[pool] case class Member(
  channel: JsChannel,
  userId: Option[String],
  troll: Boolean) extends SocketMember

private[pool] object Member {
  def apply(channel: JsChannel, user: Option[User]): Member = Member(
    channel = channel,
    userId = user map (_.id),
    troll = user.??(_.troll))
}

private[pool] case class Join(
  uid: String,
  user: Option[User],
  version: Int)

private[pool] case class Talk(tourId: String, u: String, t: String, troll: Boolean)

private[pool] case class Connected(enumerator: JsEnumerator, member: Member)

private[pool] case object GetPool
private[pool] case class Enter(user: User)
private[pool] case class Leave(userId: String)
private[pool] case class DoFinishGame(game: Game, white: User, black: User)
private[pool] case object CheckWave
private[pool] case object CheckPlayers
private[pool] case object CheckLeaders
private[pool] case object EjectLeavers
private[pool] case object RemindPlayers
private[pool] case class AddPairings(pairings: List[PairingWithGame])
private[pool] case class UpdateUsers(users: List[User])
private[pool] case class Preload(games: List[Game], users: List[User])
case class RemindPool(pool: Pool)

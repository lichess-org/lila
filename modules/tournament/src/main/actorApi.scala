package lila.tournament
package actorApi

import scala.concurrent.Promise

import lila.game.Game
import lila.socket.Socket.{ Uid, SocketVersion }
import lila.socket.SocketMember
import lila.user.User

private[tournament] case class Member(
    channel: JsChannel,
    userId: Option[String],
    troll: Boolean
) extends SocketMember

private[tournament] object Member {
  def apply(channel: JsChannel, user: Option[User]): Member = Member(
    channel = channel,
    userId = user map (_.id),
    troll = user.??(_.troll)
  )
}

private[tournament] case class Messadata(trollish: Boolean = false)

private[tournament] case class Join(
    uid: Uid,
    user: Option[User],
    version: Option[SocketVersion],
    promise: Promise[Connected]
)
private[tournament] case class Talk(tourId: String, u: String, t: String, troll: Boolean)
private[tournament] case object Reload
private[tournament] case class StartGame(game: Game)
private[tournament] case class Connected(enumerator: JsEnumerator, member: Member)

case class TournamentTable(tours: List[Tournament])

private[tournament] case object ScheduleNow
private[tournament] case object NotifyCrowd
private[tournament] case object NotifyReload

private[tournament] case class GetWaitingUsersP(promise: Promise[WaitingUsers])

private[tournament] case class SetTournamentClock(clock: chess.Clock.Config)

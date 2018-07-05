package lidraughts.team

import lidraughts.notify.Notification.Notifies
import lidraughts.notify.TeamJoined.{ Id => TJId, Name => TJName }
import lidraughts.notify.TeamMadeOwner.{ Id => TMOId, Name => TMOName }
import lidraughts.notify.{ Notification, TeamJoined, TeamMadeOwner, NotifyApi }
import lidraughts.user.User

private[team] final class Notifier(notifyApi: NotifyApi) {

  def acceptRequest(team: Team, request: Request) = {
    val notificationContent = TeamJoined(TJId(team.id), TJName(team.name))
    val notification = Notification.make(Notifies(request.user), notificationContent)

    notifyApi.addNotification(notification)
  }

  def madeOwner(team: Team, newOwner: String) = {
    val notificationContent = TeamMadeOwner(TMOId(team.id), TMOName(team.name))
    val notification = Notification.make(Notifies(newOwner), notificationContent)

    notifyApi.addNotification(notification)
  }
}

package lila.team

import lila.notify.Notification.Notifies
import lila.notify.TeamJoined.{ Id => TJId, Name => TJName }
import lila.notify.TeamMadeOwner.{ Id => TMOId, Name => TMOName }
import lila.notify.{ Notification, TeamJoined, TeamMadeOwner, NotifyApi }
import lila.user.User

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

package lidraughts.team

import lidraughts.notify.Notification.Notifies
import lidraughts.notify.TeamJoined.{ Id, Name }
import lidraughts.notify.{ Notification, TeamJoined, NotifyApi }

private[team] final class Notifier(notifyApi: NotifyApi) {

  def acceptRequest(team: Team, request: Request) = {
    val notificationContent = TeamJoined(Id(team.id), Name(team.name))
    val notification = Notification.make(Notifies(request.user), notificationContent)

    notifyApi.addNotification(notification)
  }
}

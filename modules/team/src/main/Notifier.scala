package lila.team

import lila.notify.Notification.Notifies
import lila.notify.TeamJoined.{ Id => TJId, Name => TJName }
import lila.notify.{ Notification, NotifyApi, TeamJoined }

final private[team] class Notifier(notifyApi: NotifyApi) {

  def acceptRequest(team: Team, request: Request) = {
    val notificationContent = TeamJoined(TJId(team.id), TJName(team.name))
    val notification        = Notification.make(Notifies(request.user), notificationContent)

    notifyApi.addNotification(notification)
  }
}

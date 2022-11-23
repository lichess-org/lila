package lila.team

import lila.notify.Notification.Notifies
import lila.notify.{ Notification, NotifyApi, TeamJoined }

final private class Notifier(notifyApi: NotifyApi):

  def acceptRequest(team: Team, request: Request) =
    val notificationContent = TeamJoined(team.id, team.name)
    val notification        = Notification.make(Notifies(request.user), notificationContent)

    notifyApi.addNotification(notification)

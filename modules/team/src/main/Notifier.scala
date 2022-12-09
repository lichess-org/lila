package lila.team

import lila.notify.Notification.Notifies
import lila.notify.{ Notification, NotifyApi, TeamJoined }

final private class Notifier(notifyApi: NotifyApi):

  def acceptRequest(team: Team, request: Request) =
    val notification = Notification.make(request.user, TeamJoined(team.id, team.name))
    notifyApi.addNotification(notification)

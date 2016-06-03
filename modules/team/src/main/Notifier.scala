package lila.team

import akka.actor.ActorSelection
import lila.notify.Notification.Notifies
import lila.notify.TeamJoined.{Id, Name}
import lila.notify.{Notification, TeamJoined, NotifyApi}

private[team] final class Notifier(notifyApi: NotifyApi) {

  def acceptRequest(team: Team, request: Request) = {
    val notificationContent = TeamJoined(Id(team.id), Name(team.name))
    val notification = Notification(Notifies(request.user), notificationContent)

    notifyApi.addNotification(notification)
  }
}

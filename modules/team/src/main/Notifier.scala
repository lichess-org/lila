package lila.team

import akka.actor.ActorSelection
import lila.notify.Notification.Notifies
import lila.notify.TeamJoined.{TeamId, TeamName}
import lila.notify.{Notification, TeamJoined, NotifyApi}

private[team] final class Notifier(
    sender: String,
    notifyApi: NotifyApi,
    router: ActorSelection) {

  def acceptRequest(team: Team, request: Request) = {
    val notificationContent = TeamJoined(TeamId(team.id), TeamName(team.name))
    val notification = Notification(Notifies(request.user), notificationContent)

    notifyApi.addNotification(notification)
  }
}

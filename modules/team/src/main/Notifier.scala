package lila.team

import lila.core.notify.{ NotifyApi, NotificationContent }

final private class Notifier(notifyApi: NotifyApi):

  def acceptRequest(team: Team, request: TeamRequest) =
    notifyApi.notifyOne(request.user, NotificationContent.TeamJoined(id = team.id, name = team.name))

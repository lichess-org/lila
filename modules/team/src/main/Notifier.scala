package lila.team

import lila.core.notify.{ NotifyApi, TeamJoined }

final private class Notifier(notifyApi: NotifyApi):

  def acceptRequest(team: Team, request: TeamRequest) =
    notifyApi.notifyOne(request.user, TeamJoined(id = team.id, name = team.name))

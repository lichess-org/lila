package lila.team

import lila.notify.{ NotifyApi, TeamJoined }

final private class Notifier(notifyApi: NotifyApi):

  def acceptRequest(team: Team, request: Request) =
    notifyApi.notifyOne(request.user, TeamJoined(id = team.id, name = team.name))

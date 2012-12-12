package lila
package team

import message.LichessThread
import controllers.routes

import scalaz.effects.IO

final class TeamMessenger(
    send: LichessThread ⇒ IO[Unit],
    netBaseUrl: String) {

  def joinRequest(team: Team, request: RequestWithUser) = send(LichessThread(
    to = team.createdBy,
    subject = """%s wants to join your team %s""".format(request.user.username, team.name),
    message = """%s said:
---
%s
---

To accept or decline this join request, go to your team page %s
""".format(
      request.user.username,
      request.message,
      netBaseUrl + routes.Team.show(team.id))
  ))
}

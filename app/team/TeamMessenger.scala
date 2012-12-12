package lila
package team

import message.LichessThread
import controllers.routes

import scalaz.effects._

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
""".format(request.user.username, request.message, teamUrl(team))
  ))

  def acceptRequest(team: Team, request: Request) = send(LichessThread(
    to = request.user,
    subject = """You have joined the team %s""".format(team.name),
    message = """Congratulation, your request to join the team was accepted!

Here is the team page: %s""".format(teamUrl(team))
  ))

  def declineRequest(team: Team, request: Request) = io()

  private def teamUrl(team: Team) = netBaseUrl + routes.Team.show(team.id)
}

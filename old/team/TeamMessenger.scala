package lila.app
package team

import message.LichessThread
import controllers.routes

import scalaz.effects._

final class TeamMessenger(
    send: LichessThread ⇒ IO[Unit],
    netBaseUrl: String) {

  def acceptRequest(team: Team, request: Request) = send(LichessThread(
    to = request.user,
    subject = """You have joined the team %s""".format(team.name),
    message = """Congratulation, your request to join the team was accepted!

Here is the team page: %s""".format(teamUrl(team))
  ))

  private def teamUrl(team: Team) = netBaseUrl + routes.Team.show(team.id)
}

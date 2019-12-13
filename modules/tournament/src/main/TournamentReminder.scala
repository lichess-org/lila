package lila.tournament

import play.api.libs.json.Json

import lila.hub.actorApi.socket.SendTos
import lila.socket.Socket.makeMessage

final private class TournamentReminder {

  private val max = 50

  def apply(tour: Tournament, activeUserIds: Seq[lila.user.User.ID]) = if (!tour.pairingsClosed) {
    val userIds = {
      if (activeUserIds.size > max) scala.util.Random.shuffle(activeUserIds) take max
      else activeUserIds
    }.toSet
    if (userIds.nonEmpty)
      lila.common.Bus.publish(
        SendTos(
          userIds,
          makeMessage(
            "tournamentReminder",
            Json.obj(
              "id"   -> tour.id,
              "name" -> tour.fullName
            )
          )
        ),
        "socketUsers"
      )
  }
}

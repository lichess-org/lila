package lila.tournament

import play.api.libs.json._

import lila.common.LightUser
import lila.common.PimpedJson._
import lila.game.{ Game, GameRepo }
import lila.user.User

final class JsonView(
    getLightUser: String => Option[LightUser],
    isOnline: String => Boolean) {

  def apply(id: String): Fu[JsObject] =
    TournamentRepo byId id flatten s"No such tournament: $id" flatMap apply

  def apply(tour: Tournament): Fu[JsObject] =
    lastGames(tour) map { games =>
      Json.obj(
        "id" -> tour.id,
        "createdBy" -> tour.createdBy,
        "fullName" -> tour.fullName,
        "private" -> tour.hasPassword,
        "schedule" -> tour.schedule.map(scheduleJson),
        "variant" -> tour.variant.key,
        "players" -> tour.players.map(playerJson),
        "winner" -> tour.winner.map(_.id),
        "pairings" -> tour.pairings.map(pairingJson),
        "isOpen" -> tour.isOpen,
        "isRunning" -> tour.isRunning,
        "isFinished" -> tour.isFinished,
        "lastGames" -> games.map(gameJson)) ++ specifics(tour)
    }

  private def specifics(tour: Tournament) = tour match {
    case t: Created => Json.obj(
      "enoughPlayersToStart" -> t.enoughPlayersToStart,
      "enoughPlayersToEarlyStart" -> t.enoughPlayersToEarlyStart,
      "missingPlayers" -> (t.missingPlayers != -1).option(t.missingPlayers)
    ).noNull
    case t: Started => Json.obj(
      "seconds" -> t.remainingSeconds)
    case _ => Json.obj()
  }

  private def lastGames(tour: Tournament) = tour match {
    case t: StartedOrFinished => GameRepo.games(t recentGameIds 4)
    case _                    => fuccess(Nil)
  }

  private def scheduleJson(s: Schedule) = Json.obj(
    "seconds" -> s.inSeconds)

  private def gameUserJson(player: lila.game.Player) = {
    val light = player.userId flatMap getLightUser
    Json.obj(
      "id" -> player.userId,
      "name" -> light.map(_.name),
      "title" -> light.map(_.title),
      "rating" -> player.rating
    ).noNull
  }

  private def gameJson(g: Game) = Json.obj(
    "id" -> g.id,
    "fen" -> (chess.format.Forsyth exportBoard g.toChess.board),
    "color" -> g.firstColor.name,
    "lastMove" -> ~g.castleLastMoveTime.lastMoveString,
    "user1" -> gameUserJson(g.firstPlayer),
    "user2" -> gameUserJson(g.secondPlayer))

  private def playerJson(p: Player) = {
    val light = getLightUser(p.id)
    Json.obj(
      "id" -> p.id,
      "username" -> light.map(_.name),
      "title" -> light.map(_.title),
      "online" -> isOnline(p.id).option(true),
      "rating" -> p.rating,
      "withdraw" -> p.withdraw.option(true),
      "score" -> p.score).noNull
  }

  private def pairingJson(p: Pairing) = Json.obj(
    "gameId" -> p.gameId,
    "status" -> p.status.name,
    "user1" -> p.user1,
    "user2" -> p.user2,
    "winner" -> p.winner)
}

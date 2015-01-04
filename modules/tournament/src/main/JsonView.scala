package lila.tournament

import play.api.libs.json._

import lila.game.{ Game, GameRepo }
import lila.user.User

final class JsonView {

  def apply(
    tour: Tournament,
    apiVersion: Int): Fu[JsObject] = lastGames(tour) map { games =>
    Json.obj(
      "id" -> tour.id,
      "variant" -> tour.variant.key,
      "players" -> tour.players.map(playerJson),
      "winner" -> tour.winner.map(_.id),
      "pairings" -> tour.pairings.map(pairingJson),
      "isOpen" -> tour.isOpen,
      "isRunning" -> tour.isRunning,
      "isFinished" -> tour.isFinished,
      "lastGames" -> games.map(gameJson),
      "url" -> Json.obj(
        "socket" -> s"/tournament/${tour.id}/socket/v$apiVersion"))
  }

  private def lastGames(tour: Tournament) = tour match {
    case t: StartedOrFinished => GameRepo.games(t recentGameIds 4)
    case _                    => fuccess(Nil)
  }

  private def gameJson(g: Game) = Json.obj(
    "id" -> g.id,
    "fen" -> (chess.format.Forsyth exportBoard g.toChess.board),
    "color" -> g.firstColor.name,
    "lastMove" -> ~g.castleLastMoveTime.lastMoveString)

  private def playerJson(p: Player) = Json.obj(
    "id" -> p.id,
    "rating" -> p.rating,
    "withdraw" -> p.withdraw,
    "score" -> p.score)

  private def pairingJson(p: Pairing) = Json.obj(
    "gameId" -> p.gameId,
    "status" -> p.status.name,
    "user1" -> p.user1,
    "user2" -> p.user2,
    "winner" -> p.winner)
}

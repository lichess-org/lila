package lila.tournament

import play.api.libs.json._

import lila.common.LightUser
import lila.common.PimpedJson._
import lila.game.{ Game, GameRepo }
import lila.user.User

final class JsonView(
    getLightUser: String => Option[LightUser]) {

  def apply(id: String): Fu[JsObject] =
    TournamentRepo byId id flatten s"No such tournament: $id" flatMap apply

  def apply(tour: Tournament): Fu[JsObject] =
    lastGames(tour) map { games =>
      val sheets = tour.system.scoringSystem scoreSheets tour
      Json.obj(
        "id" -> tour.id,
        "createdBy" -> tour.createdBy,
        "system" -> tour.system.toString.toLowerCase,
        "fullName" -> tour.fullName,
        "private" -> tour.hasPassword,
        "schedule" -> tour.schedule.map(scheduleJson),
        "variant" -> tour.variant.key,
        "players" -> tour.rankedPlayers.map((playerJson(sheets) _).tupled),
        "winner" -> tour.winner.map(_.id),
        "pairings" -> tour.pairings.take(50).map(pairingJson),
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

  private def sheetJson(sheet: ScoreSheet) = sheet match {
    case s: arena.ScoringSystem.Sheet => Json.obj(
      "scores" -> s.scores.take(20).reverse.map { score =>
        if (score.flag == arena.ScoringSystem.Normal) Json.arr(score.value)
        else Json.arr(score.value, score.flag.toString.toLowerCase)
      },
      "total" -> s.total,
      "fire" -> s.onFire)
    case s: swiss.SwissSystem.Sheet => Json.obj(
      "scores" -> s.scores.take(20).reverse.map(_.repr),
      "total" -> s.totalRepr,
      "neustadtl" -> s.neustadtlRepr)
  }

  private def playerJson(sheets: Map[String, ScoreSheet])(rank: Int, p: Player) = {
    val light = getLightUser(p.id)
    Json.obj(
      "rank" -> rank,
      "id" -> p.id,
      "username" -> light.map(_.name),
      "title" -> light.map(_.title),
      "rating" -> p.rating,
      "withdraw" -> p.withdraw.option(true),
      "score" -> p.score,
      "sheet" -> sheets.get(p.id).map(sheetJson)).noNull
  }

  private def pairingUserJson(userId: String) = {
    val name = getLightUser(userId).fold(userId)(_.name)
    if (name == userId) Json.arr(userId)
    else Json.arr(userId, name)
  }

  private def pairingJson(p: Pairing) = Json.obj(
    "gameId" -> p.gameId,
    "status" -> p.status.id,
    "user1" -> pairingUserJson(p.user1),
    "user2" -> pairingUserJson(p.user2),
    "winner" -> p.winner)
}

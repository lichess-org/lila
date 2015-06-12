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

  def apply(tour: Tournament): Fu[JsObject] = for {
    rankedPlayers <- PlayerRepo.bestByTourWithRank(tour.id, 10)
    pairings <- PairingRepo.recentByTour(tour.id, 40)
    games <- GameRepo games pairings.map(_.gameId)
    sheets <- rankedPlayers.map { p =>
      tour.system.scoringSystem.sheet(tour, p.player.userId) map p.player.userId.->
    }.sequenceFu.map(_.toMap)
  } yield {
    Json.obj(
      "id" -> tour.id,
      "createdBy" -> tour.createdBy,
      "system" -> tour.system.toString.toLowerCase,
      "fullName" -> tour.fullName,
      "private" -> tour.`private`,
      "variant" -> tour.variant.key,
      "players" -> rankedPlayers.map(playerJson(sheets, tour)),
      "pairings" -> pairings.map(pairingJson),
      "isStarted" -> tour.isStarted,
      "isFinished" -> tour.isFinished,
      "lastGames" -> games.map(gameJson),
      "schedule" -> tour.schedule.map(scheduleJson)) ++ specifics(tour)
  }

  private def specifics(tour: Tournament) = tour match {
    case t if t.isCreated => Json.obj(
      "secondsToStart" -> t.secondsToStart,
      "startsAt" -> org.joda.time.format.ISODateTimeFormat.dateTime.print(t.startsAt))
    case t if t.isStarted => Json.obj("secondsToFinish" -> t.secondsToFinish)
    case _                => Json.obj()
  }

  private def gameUserJson(player: lila.game.Player) = {
    val light = player.userId flatMap getLightUser
    Json.obj(
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

  private def scheduleJson(s: Schedule) = Json.obj(
    "freq" -> s.freq.name,
    "speed" -> s.speed.name)

  private def sheetJson(sheet: ScoreSheet) = sheet match {
    case s: arena.ScoringSystem.Sheet => Json.obj(
      "scores" -> s.scores.reverse.map { score =>
        if (score.flag == arena.ScoringSystem.Normal) JsNumber(score.value)
        else Json.arr(score.value, score.flag.id)
      },
      "total" -> s.total,
      "fire" -> s.onFire.option(true)
    ).noNull
  }

  private def playerJson(sheets: Map[String, ScoreSheet], tour: Tournament)(rankedPlayer: RankedPlayer) = {
    val p = rankedPlayer.player
    val light = getLightUser(p.userId)
    Json.obj(
      "rank" -> rankedPlayer.rank,
      "name" -> light.fold(p.userId)(_.name),
      "title" -> light.map(_.title),
      "rating" -> p.rating,
      "provisional" -> p.provisional.option(true),
      "withdraw" -> p.withdraw.option(true),
      "score" -> p.score,
      "perf" -> p.perf,
      "opposition" -> none[Int], //(tour.isFinished && rankedPlayer.rank <= 3).option(opposition(tour, p)),
      "sheet" -> sheets.get(p.userId).map(sheetJson)
    ).noNull
  }

  // private def opposition(tour: Tournament, p: Player): Int =
  //   tour.userPairings(p.id).foldLeft((0, 0)) {
  //     case ((count, sum), pairing) => (
  //       count + 1,
  //       (pairing opponentOf p.id flatMap tour.playerByUserId).fold(sum)(_.rating + sum)
  //     )
  //   } match {
  //     case (0, _)       => 0
  //     case (count, sum) => sum / count
  //   }

  private def pairingUserJson(userId: String) = getLightUser(userId).fold(userId)(_.name)

  private def pairingJson(p: Pairing) = Json.obj(
    "id" -> p.gameId,
    "st" -> p.status.id,
    "u1" -> pairingUserJson(p.user1),
    "u2" -> pairingUserJson(p.user2),
    "wi" -> p.winner)
}

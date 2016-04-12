package lila.app
package mashup

import lila.api.Context
import lila.common.LightUser
import lila.forum.MiniForumPost
import lila.game.{ Game, Pov, GameRepo }
import lila.playban.TempBan
import lila.rating.PerfType
import lila.simul.Simul
import lila.timeline.Entry
import lila.tournament.{ Tournament, Winner }
import lila.tv.{ Tv, StreamOnAir }
import lila.user.User
import play.api.libs.json._

final class Preload(
    tv: Tv,
    leaderboard: Boolean => Fu[List[User.LightPerf]],
    tourneyWinners: Int => Fu[List[Winner]],
    timelineEntries: String => Fu[List[Entry]],
    streamsOnAir: () => Fu[List[StreamOnAir]],
    dailyPuzzle: () => Fu[Option[lila.puzzle.DailyPuzzle]],
    countRounds: () => Int,
    donationProgress: () => Fu[lila.donation.Progress],
    lobbyApi: lila.api.LobbyApi,
    getPlayban: String => Fu[Option[TempBan]],
    lightUser: String => Option[LightUser]) {

  private type Response = (JsObject, List[Entry], List[MiniForumPost], List[Tournament], List[Simul], Option[Game], List[User.LightPerf], List[Winner], Option[lila.puzzle.DailyPuzzle], List[StreamOnAir], List[lila.blog.MiniPost], Option[TempBan], Option[Preload.CurrentGame], Int, lila.donation.Progress)

  def apply(
    posts: Fu[List[MiniForumPost]],
    tours: Fu[List[Tournament]],
    simuls: Fu[List[Simul]])(implicit ctx: Context): Fu[Response] =
    lobbyApi(ctx) zip
      posts zip
      tours zip
      simuls zip
      tv.getBest zip
      (ctx.userId ?? timelineEntries) zip
      leaderboard(true) zip
      tourneyWinners(10) zip
      dailyPuzzle() zip
      streamsOnAir() zip
      (ctx.userId ?? getPlayban) zip
      (ctx.me ?? Preload.currentGame(lightUser)) zip
      donationProgress() map {
        case ((((((((((((data, posts), tours), simuls), feat), entries), lead), tWinners), puzzle), streams), playban), currentGame),
          progress) =>
          (data, entries, posts, tours, simuls, feat, lead, tWinners, puzzle, streams, Env.blog.lastPostCache.apply, playban, currentGame, countRounds(), progress)
      }
}

object Preload {

  case class CurrentGame(pov: Pov, json: JsObject, opponent: String)

  def currentGame(lightUser: String => Option[LightUser])(user: User) =
    GameRepo.urgentGames(user) map { povs =>
      povs.find { p =>
        p.game.nonAi && p.game.hasClock && p.isMyTurn
      } map { pov =>
        val opponent = lila.game.Namer.playerString(pov.opponent)(lightUser)
        CurrentGame(
          pov = pov,
          opponent = opponent,
          json = Json.obj(
            "id" -> pov.game.id,
            "color" -> pov.color.name,
            "opponent" -> opponent))
      }
    }
}

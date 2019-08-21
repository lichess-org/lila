package lila.app
package mashup

import lila.api.Context
import lila.event.Event
import lila.forum.MiniForumPost
import lila.game.{ Game, Pov, GameRepo }
import lila.playban.TempBan
import lila.simul.Simul
import lila.streamer.LiveStreams
import lila.timeline.Entry
import lila.tournament.{ Tournament, Winner }
import lila.tv.Tv
import lila.user.LightUserApi
import lila.user.User
import play.api.libs.json._

final class Preload(
    tv: Tv,
    leaderboard: Unit => Fu[List[User.LightPerf]],
    tourneyWinners: Fu[List[Winner]],
    timelineEntries: String => Fu[Vector[Entry]],
    liveStreams: () => Fu[LiveStreams],
    dailyPuzzle: lila.puzzle.Daily.Try,
    countRounds: () => Int,
    lobbyApi: lila.api.LobbyApi,
    getPlayban: User.ID => Fu[Option[TempBan]],
    lightUserApi: LightUserApi,
    roundProxyPov: (Game.ID, User) => Fu[Option[Pov]],
    urgentGames: User => Fu[List[Pov]]
) {

  import Preload._

  private type Response = (JsObject, Vector[Entry], List[MiniForumPost], List[Tournament], List[Event], List[Simul], Option[Game], List[User.LightPerf], List[Winner], Option[lila.puzzle.DailyPuzzle], LiveStreams.WithTitles, List[lila.blog.MiniPost], Option[TempBan], Option[Preload.CurrentGame], Int, List[Pov])

  def apply(
    posts: Fu[List[MiniForumPost]],
    tours: Fu[List[Tournament]],
    events: Fu[List[Event]],
    simuls: Fu[List[Simul]]
  )(implicit ctx: Context): Fu[Response] =
    lobbyApi(ctx) zip
      posts zip
      tours zip
      events zip
      simuls zip
      tv.getBestGame zip
      (ctx.userId ?? timelineEntries) zip
      leaderboard(()) zip
      tourneyWinners zip
      (ctx.noBot ?? dailyPuzzle()) zip
      liveStreams().dmap(_.autoFeatured withTitles lightUserApi) zip
      (ctx.userId ?? getPlayban) zip
      (ctx.blind ?? ctx.me ?? urgentGames) flatMap {
        case (data, povs) ~ posts ~ tours ~ events ~ simuls ~ feat ~ entries ~ lead ~ tWinners ~ puzzle ~ streams ~ playban ~ blindGames =>
          (ctx.me ?? currentGameMyTurn(povs, lightUserApi.sync) _) flatMap { currentGame =>
            lightUserApi.preloadMany {
              tWinners.map(_.userId) :::
                posts.flatMap(_.userId) :::
                entries.flatMap(_.userIds).toList
            } inject
              (data, entries, posts, tours, events, simuls, feat, lead, tWinners, puzzle, streams, Env.blog.lastPostCache.apply, playban, currentGame, countRounds(), blindGames)
          }
      }

  def currentGameMyTurn(user: User): Fu[Option[CurrentGame]] =
    GameRepo.playingRealtimeNoAi(user, 10).flatMap {
      _.map { roundProxyPov(_, user) }.sequenceFu.map(_.flatten)
    } flatMap {
      currentGameMyTurn(_, lightUserApi.sync)(user)
    }

  private def currentGameMyTurn(povs: List[Pov], lightUser: lila.common.LightUser.GetterSync)(user: User): Fu[Option[CurrentGame]] =
    ~povs.collectFirst {
      case p1 if p1.game.nonAi && p1.game.hasClock && p1.isMyTurn =>
        roundProxyPov(p1.gameId, user) map (_ | p1) map { pov =>
          val opponent = lila.game.Namer.playerText(pov.opponent)(lightUser)
          CurrentGame(
            pov = pov,
            opponent = opponent,
            json = Json.obj(
              "id" -> pov.gameId,
              "color" -> pov.color.name,
              "opponent" -> opponent
            )
          ).some
        }
    }
}

object Preload {

  case class CurrentGame(pov: Pov, json: JsObject, opponent: String)
}

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
    userCached: lila.user.Cached,
    tourWinners: lila.tournament.WinnersApi,
    timelineApi: lila.timeline.EntryApi,
    liveStreamApi: lila.streamer.LiveStreamApi,
    dailyPuzzle: lila.puzzle.Daily.Try,
    lobbyApi: lila.api.LobbyApi,
    playbanApi: lila.playban.PlaybanApi,
    lightUserApi: LightUserApi,
    roundProxy: lila.round.GameProxyRepo
) {

  import Preload._

  private type Response = (JsObject, Vector[Entry], List[MiniForumPost], List[Tournament], List[Event], List[Simul], Option[Game], List[User.LightPerf], List[Winner], Option[lila.puzzle.DailyPuzzle], LiveStreams.WithTitles, List[lila.blog.MiniPost], Option[TempBan], Option[Preload.CurrentGame], List[Pov])

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
      (ctx.userId ?? timelineApi.userEntries) zip
      userCached.topWeek(()) zip
      tourWinners.all.dmap(_.top) zip
      (ctx.noBot ?? dailyPuzzle()) zip
      liveStreamApi.all.dmap(_.autoFeatured withTitles lightUserApi) zip
      (ctx.userId ?? playbanApi.currentBan) zip
      (ctx.blind ?? ctx.me ?? roundProxy.urgentGames) flatMap {
        case (data, povs) ~ posts ~ tours ~ events ~ simuls ~ feat ~ entries ~ lead ~ tWinners ~ puzzle ~ streams ~ playban ~ blindGames =>
          (ctx.me ?? currentGameMyTurn(povs, lightUserApi.sync) _) flatMap { currentGame =>
            lightUserApi.preloadMany {
              tWinners.map(_.userId) :::
                posts.flatMap(_.userId) :::
                entries.flatMap(_.userIds).toList
            } inject ((
              data,
              entries,
              posts,
              tours,
              events,
              simuls,
              feat,
              lead,
              tWinners,
              puzzle,
              streams.excludeUsers(events.flatMap(_.hostedBy)),
              Env.blog.lastPostCache.apply,
              playban,
              currentGame,
              blindGames
            ))
          }
      }

  def currentGameMyTurn(user: User): Fu[Option[CurrentGame]] =
    GameRepo.playingRealtimeNoAi(user, 10).flatMap {
      _.map { roundProxy.pov(_, user) }.sequenceFu.dmap(_.flatten)
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

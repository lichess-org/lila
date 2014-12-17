package lila.app
package mashup

import akka.actor.ActorRef
import akka.pattern.ask
import play.api.libs.json.{ Json, JsObject, JsArray }

import controllers.routes
import lila.api.Context
import lila.forum.MiniForumPost
import lila.game.{ Game, GameRepo, Pov }
import lila.lobby.actorApi.GetOpen
import lila.lobby.{ Hook, HookRepo, Seek, SeekApi }
import lila.rating.PerfType
import lila.setup.FilterConfig
import lila.socket.History
import lila.timeline.Entry
import lila.tournament.{ Enterable, Winner }
import lila.tv.{ Featured, StreamOnAir }
import lila.user.User
import makeTimeout.large

final class Preload(
    lobby: ActorRef,
    lobbyVersion: () => Int,
    featured: Featured,
    leaderboard: Boolean => Fu[List[(User, PerfType)]],
    tourneyWinners: Int => Fu[List[Winner]],
    timelineEntries: String => Fu[List[Entry]],
    streamsOnAir: => () => Fu[List[StreamOnAir]],
    dailyPuzzle: () => Fu[Option[lila.puzzle.DailyPuzzle]],
    countRounds: () => Int,
    seekApi: SeekApi) {

  private type Response = (JsObject, List[Entry], List[MiniForumPost], List[Enterable], Option[Game], List[(User, PerfType)], List[Winner], Option[lila.puzzle.DailyPuzzle], List[Pov], List[Seek], List[StreamOnAir], Int)

  def apply(
    posts: Fu[List[MiniForumPost]],
    tours: Fu[List[Enterable]],
    filter: Fu[FilterConfig])(implicit ctx: Context): Fu[Response] =
    (lobby ? GetOpen(ctx.me)).mapTo[List[Hook]] zip
      posts zip
      tours zip
      featured.one zip
      (ctx.userId ?? timelineEntries) zip
      leaderboard(true) zip
      tourneyWinners(10) zip
      dailyPuzzle() zip
      (ctx.me ?? GameRepo.nowPlaying) zip
      seekApi.all zip
      filter zip
      streamsOnAir() map {
        case (((((((((((hooks, posts), tours), feat), entries), lead), tWinners), puzzle), povs), seeks), filter), streams) =>
          (Json.obj(
            "version" -> lobbyVersion(),
            "hookPool" -> JsArray(hooks map (_.render)),
            "filter" -> filter.render
          ), entries, posts, tours, feat, lead, tWinners, puzzle, povs, seeks, streams, countRounds())
      }
}

package lila.app
package mashup

import akka.actor.ActorRef
import akka.pattern.ask
import play.api.libs.json.{ Json, JsObject, JsArray }
import play.api.mvc.Call

import controllers.routes
import lila.api.Context
import lila.forum.MiniForumPost
import lila.game.{ Game, GameRepo, Pov }
import lila.lobby.actorApi.GetOpen
import lila.lobby.{ Hook, HookRepo }
import lila.pool.Pool
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
    nowPlaying: (User, Int) => Fu[List[Pov]],
    streamsOnAir: => () => Fu[List[StreamOnAir]],
    dailyPuzzle: () => Fu[Option[lila.puzzle.DailyPuzzle]],
    getPools: () => Fu[List[Pool]],
    countRounds: () => Fu[Int]) {

  private type Response = (JsObject, List[Entry], List[MiniForumPost], List[Enterable], Option[Game], List[(User, PerfType)], List[Winner], Option[lila.puzzle.DailyPuzzle], List[Pov], List[Pool], List[StreamOnAir], Int)

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
      (ctx.me ?? { nowPlaying(_, 3) }) zip
      filter zip
      getPools() zip
      streamsOnAir() zip
      countRounds() map {
        case ((((((((((((hooks, posts), tours), feat), entries), lead), tWinners), puzzle), playing), filter), pools), streams), nbRounds) =>
          (Json.obj(
            "version" -> lobbyVersion(),
            "pool" -> JsArray(hooks map (_.render)),
            "filter" -> filter.render
          ), entries, posts, tours, feat, lead, tWinners, puzzle, playing, pools, streams, nbRounds)
      }
}

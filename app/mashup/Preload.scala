package lila.app
package mashup

import akka.actor.ActorRef
import akka.pattern.ask
import play.api.libs.json.{ Json, JsObject, JsArray }
import play.api.mvc.Call

import controllers.routes
import lila.api.Context
import lila.forum.PostLiteView
import lila.game.{ Game, GameRepo, Pov }
import lila.lobby.actorApi.GetOpen
import lila.lobby.{ Hook, HookRepo }
import lila.pool.Pool
import lila.relation.RelationApi
import lila.setup.FilterConfig
import lila.socket.History
import lila.timeline.Entry
import lila.tournament.{ Enterable, Winner }
import lila.tv.{ Featured, StreamOnAir }
import lila.user.User
import makeTimeout.large

final class Preload(
    lobby: ActorRef,
    history: History,
    featured: Featured,
    relations: RelationApi,
    leaderboard: Int => Fu[List[User]],
    tourneyWinners: Int => Fu[List[Winner]],
    timelineEntries: String => Fu[List[Entry]],
    nowPlaying: (User, Int) => Fu[List[Pov]],
    streamsOnAir: => () => Fu[List[StreamOnAir]],
    dailyPuzzle: () => Fu[Option[lila.puzzle.DailyPuzzle]],
    getPools: () => Fu[List[Pool]]) {

  private type RightResponse = (JsObject, List[Entry], List[PostLiteView], List[Enterable], Option[Game], List[User], List[Winner], Option[lila.puzzle.DailyPuzzle], List[Pov], List[Pool], List[StreamOnAir])
  private type Response = Either[Call, RightResponse]

  def apply(
    posts: Fu[List[PostLiteView]],
    tours: Fu[List[Enterable]],
    filter: Fu[FilterConfig])(implicit ctx: Context): Fu[Response] =
    (lobby ? GetOpen).mapTo[List[Hook]] zip
      posts zip
      tours zip
      featured.one zip
      (ctx.userId ?? relations.blocks) zip
      (ctx.userId ?? timelineEntries) zip
      leaderboard(10) zip
      tourneyWinners(10) zip
      dailyPuzzle() zip
      (ctx.me ?? { nowPlaying(_, 3) }) zip
      filter zip
      getPools() zip
      streamsOnAir() map {
        case ((((((((((((hooks, posts), tours), feat), blocks), entries), lead), tWinners), puzzle), playing), filter), pools), streams) =>
          (Right((Json.obj(
            "version" -> history.version,
            "pool" -> JsArray(hooks map (_.render)),
            "filter" -> filter.render,
            "blocks" -> blocks,
            "engine" -> ctx.me.??(_.engine)
          ), entries, posts, tours, feat, lead, tWinners, puzzle, playing, pools, streams)))
      }
}

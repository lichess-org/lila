package lila.app
package mashup

import lila.api.Context
import lila.forum.MiniForumPost
import lila.game.Game
import lila.rating.PerfType
import lila.timeline.Entry
import lila.tournament.{ Enterable, Winner }
import lila.tv.{ Featured, StreamOnAir }
import lila.user.User
import play.api.libs.json.JsObject

final class Preload(
    featured: Featured,
    leaderboard: Boolean => Fu[List[(User, PerfType)]],
    tourneyWinners: Int => Fu[List[Winner]],
    timelineEntries: String => Fu[List[Entry]],
    streamsOnAir: => () => Fu[List[StreamOnAir]],
    dailyPuzzle: () => Fu[Option[lila.puzzle.DailyPuzzle]],
    countRounds: () => Int,
    lobbyApi: lila.api.LobbyApi) {

  private type Response = (JsObject, List[Entry], List[MiniForumPost], List[Enterable], Option[Game], List[(User, PerfType)], List[Winner], Option[lila.puzzle.DailyPuzzle], List[StreamOnAir], List[lila.blog.MiniPost], Int)

  def apply(
    posts: Fu[List[MiniForumPost]],
    tours: Fu[List[Enterable]])(implicit ctx: Context): Fu[Response] =
    lobbyApi(ctx) zip
      posts zip
      tours zip
      featured.one zip
      (ctx.userId ?? timelineEntries) zip
      leaderboard(true) zip
      tourneyWinners(10) zip
      dailyPuzzle() zip
      streamsOnAir() map {
        case ((((((((data, posts), tours), feat), entries), lead), tWinners), puzzle), streams) =>
          (data, entries, posts, tours, feat, lead, tWinners, puzzle, streams, Env.blog.lastPostCache.apply, countRounds())
      }
}

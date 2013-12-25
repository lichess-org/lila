package lila.app
package mashup

import akka.actor.ActorRef
import akka.pattern.ask
import play.api.libs.json.{ Json, JsObject, JsArray }
import play.api.mvc.Call

import controllers.routes
import lila.forum.PostLiteView
import lila.game.{ Game, GameRepo, Featured }
import lila.lobby.actorApi.GetOpen
import lila.lobby.{ Hook, HookRepo }
import lila.setup.FilterConfig
import lila.socket.History
import lila.timeline.Entry
import lila.tournament.Created
import lila.user.{ User, Context }
import lila.relation.RelationApi
import makeTimeout.large

final class Preload(
    lobby: ActorRef,
    history: History,
    featured: Featured,
    relations: RelationApi,
    leaderboard: Int => Fu[List[User]],
    progress: Int => Fu[List[User]],
    timelineEntries: String ⇒ Fu[List[Entry]]) {

  private type RightResponse = (JsObject, List[Entry], List[PostLiteView], List[Created], Option[Game], List[User], List[User])
  private type Response = Either[Call, RightResponse]

  def apply(
    posts: Fu[List[PostLiteView]],
    tours: Fu[List[Created]],
    filter: Fu[FilterConfig])(implicit ctx: Context): Fu[Response] =
    (lobby ? GetOpen).mapTo[List[Hook]] zip
      posts zip
      tours zip
      featured.one zip
      (ctx.userId ?? relations.blocks) zip
      (ctx.userId ?? timelineEntries) zip
      leaderboard(10) zip
      progress(10) zip
      filter map {
        case ((((((((hooks, posts), tours), feat), blocks), entries), leaderboard), progress), filter) ⇒
          (Right((Json.obj(
            "version" -> history.version,
            "pool" -> JsArray(hooks map (_.render)),
            "filter" -> filter.render,
            "blocks" -> blocks
          ), entries, posts, tours, feat, leaderboard, progress)))
      }
}

package lila.app
package mashup

import akka.actor.ActorRef
import akka.pattern.ask
import play.api.libs.json.{ Json, JsObject, JsArray }
import play.api.mvc.Call

import controllers.routes
import lila.forum.PostLiteView
import lila.game.{ Game, GameRepo, Featured }
import lila.lobby.actorApi.lobby._
import lila.lobby.{ Hook, HookRepo }
import lila.setup.FilterConfig
import lila.socket.History
import lila.timeline.{ Entry, GameEntry }
import lila.tournament.Created
import lila.user.{ User, Context }
import makeTimeout.large

final class Preload(
    lobby: ActorRef,
    history: History,
    featured: Featured,
    recentGames: () ⇒ Fu[List[GameEntry]],
    timelineEntries: String ⇒ Fu[List[Entry]]) {

  private type RightResponse = (JsObject, List[Entry], List[GameEntry], List[PostLiteView], List[Created], Option[Game])
  private type Response = Either[Call, RightResponse]

  def apply(
    posts: Fu[List[PostLiteView]],
    tours: Fu[List[Created]],
    filter: Fu[FilterConfig])(implicit ctx: Context): Fu[Response] =
    ctx.isAuth.fold(lobby ? GetOpen, lobby ? GetOpenCasual).mapTo[List[Hook]] zip
      recentGames() zip
      posts zip
      tours zip
      featured.one zip
      (ctx.userId ?? timelineEntries) zip
      filter map {
        case ((((((hooks, gameEntries), posts), tours), feat), entries), filter) ⇒
          (Right((Json.obj(
            "version" -> history.version,
            "pool" -> JsArray(hooks map (_.render)),
            "filter" -> filter.render
          ), entries, gameEntries, posts, tours, feat)))
      }
}

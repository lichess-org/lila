package lila.app
package mashup

import lila.lobby.{ Hook, HookRepo, Messenger }
import lila.lobby.actorApi.lobby._
import lila.timeline.Entry
import lila.game.{ Game, GameRepo, Featured }
import lila.forum.PostLiteView
import lila.socket.History
import lila.tournament.Created
import lila.setup.FilterConfig
import lila.user.Context
import controllers.routes
import makeTimeout.large

import akka.actor.ActorRef
import akka.pattern.ask
import play.api.mvc.Call
import play.api.libs.json.{ Json, JsObject, JsArray }

final class Preload(
    lobby: ActorRef,
    messenger: Messenger,
    history: History,
    featured: Featured) {

  private type RightResponse = (JsObject, List[Entry], List[PostLiteView], List[Created], Option[Game])
  private type Response = Either[Call, RightResponse]

  def apply(
    timeline: Fu[List[Entry]],
    posts: Fu[List[PostLiteView]],
    tours: Fu[List[Created]],
    filter: Fu[FilterConfig])(implicit ctx: Context): Fu[Response] =
    ctx.isAuth.fold(lobby ? GetOpen, lobby ? GetOpenCasual).mapTo[List[Hook]] zip
      (ctx.canSeeChat ?? messenger.recent(ctx.troll, 20)) zip
      timeline zip posts zip tours zip featured.one zip filter map {
        case ((((((hooks, messages), entries), posts), tours), feat), filter) ⇒
          (Right((Json.obj(
            "version" -> history.version,
            "pool" -> renderHooks(hooks),
            "chat" -> (messages.reverse map (_.render)),
            "filter" -> filter.render
          ), entries, posts, tours, feat)))
      }

  private def renderHooks(hooks: List[Hook]): JsArray = JsArray {
    hooks map (_.render)
  }
}

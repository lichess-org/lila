package lila.app
package mashup

import lila.lobby.{ Fisherman, Hook, HookRepo, MessageRepo }
import lila.timeline.Entry
import lila.game.{ Game, GameRepo, Featured }
import lila.forum.PostLiteView
import lila.socket.History
import lila.tournament.Created
import lila.setup.FilterConfig
import controllers.routes

import play.api.mvc.Call
import play.api.libs.json.{ Json, JsObject, JsArray }
import makeTimeout.short

final class Preload(
    fisherman: Fisherman,
    history: History,
    featured: Featured) {

  private type RightResponse = (JsObject, List[PostLiteView], List[Created], Option[Game])
  private type Response = Either[Call, RightResponse]

  def apply(
    auth: Boolean,
    chat: Boolean,
    myHook: Option[Hook],
    timeline: Fu[List[Entry]],
    posts: Fu[List[PostLiteView]],
    tours: Fu[List[Created]],
    filter: Fu[FilterConfig]): Fu[Response] =
    myHook.flatMap(_.gameId).fold[Fu[Response]](
      auth.fold(HookRepo.allOpen, HookRepo.allOpenCasual) zip
        (chat ?? MessageRepo.recent) zip
        timeline zip posts zip tours zip featured.one zip filter map {
          case ((((((hooks, messages), entries), posts), tours), feat), filter) ⇒ (Right((Json.obj(
            "version" -> history.version,
            "pool" -> renderHooks(hooks, myHook),
            "chat" -> (messages.reverse map (_.render)),
            "timeline" -> (entries.reverse map (_.render)),
            "filter" -> filter.render
          ), posts, tours, feat)))
        }) { gameId ⇒
        GameRepo game gameId map { gameOption ⇒
          Left(gameOption.fold(routes.Lobby.home()) { game ⇒
            routes.Round.player(game fullIdOf game.creatorColor)
          })
        }
      }

  private def renderHooks(
    hooks: List[Hook],
    myHook: Option[Hook]): JsArray = JsArray {
    hooks map { h ⇒
      myHook.exists(_.id == h.id).fold(
        h.render ++ Json.obj("ownerId" -> h.ownerId),
        h.render)
    }
  }
}

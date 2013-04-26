package lila.app
package mashup

import lila.timeline.Entry
import lila.game.{ Game, Featured }
import lila.forum.PostLiteView
import controllers.routes
import lila.socket.History
// import tournament.Created
// import setup.FilterConfig

// import play.api.mvc.Call
// import play.api.libs.concurrent.Akka
// import play.api.Play.current
// import play.api.libs.json.{ Json, JsObject, JsArray }
// import scala.concurrent.Future
// import scala.concurrent.duration._
// import akka.util.Timeout
// import scalaz.effects._

// TODO
// final class Preload(
//     fisherman: Fisherman,
//     history: History,
//     hookRepo: HookRepo,
//     getGame: String ⇒ IO[Option[Game]],
//     messageRepo: MessageRepo,
//     featured: Featured) {

//   private implicit val executor = Akka.system.dispatcher
//   private implicit val timeout = Timeout(1 second)
//   private type RightResponse = (JsObject, List[PostLiteView], List[Created], Option[Game])
//   private type Response = Either[Call, RightResponse]

//   def apply(
//     auth: Boolean,
//     chat: Boolean,
//     myHook: Option[Hook],
//     timeline: IO[List[Entry]],
//     posts: IO[List[PostLiteView]],
//     tours: IO[List[Created]], 
//     filter: IO[FilterConfig]): Future[Response] =
//     myHook.flatMap(_.gameId).fold(
//       futureHooks(auth) zip
//         futureMessages(chat) zip
//         ioToFuture(timeline) zip
//         ioToFuture(posts) zip
//         ioToFuture(tours) zip
//         featured.one zip
//         ioToFuture(filter) map {
//           case ((((((hooks, messages), entries), posts), tours), feat), filter) ⇒ (Right((Json.obj(
//             "version" -> history.version,
//             "pool" -> renderHooks(hooks, myHook),
//             "chat" -> (messages.reverse map (_.render)),
//             "timeline" -> (entries.reverse map (_.render)),
//             "filter" -> filter.render
//           ), posts, tours, feat))): Response
//         }) { gameId ⇒
//         futureGame(gameId) map { gameOption ⇒
//           Left(gameOption.fold(routes.Lobby.home()) { game ⇒
//             routes.Round.player(game fullIdOf game.creatorColor)
//           }): Response
//         }
//       }

//   private def futureHooks(auth: Boolean): Future[List[Hook]] = ioToFuture {
//     auth.fold(hookRepo.allOpen, hookRepo.allOpenCasual)
//   }

//   private def futureMessages(chat: Boolean) = ioToFuture {
//     chat.fold(messageRepo.recent, io(Nil))
//   }

//   private def futureGame(gameId: String): Future[Option[Game]] = ioToFuture {
//     getGame(gameId)
//   }

//   private def ioToFuture[A](ioa: IO[A]): Future[A] = Future {
//     ioa.unsafePerformIO
//   }

//   private def renderHooks(
//     hooks: List[Hook],
//     myHook: Option[Hook]): JsArray = JsArray {
//     hooks map { h ⇒
//       myHook.exists(_.id == h.id).fold(
//         h.render ++ Json.obj("ownerId" -> h.ownerId),
//         h.render)
//     }
//   }
// }

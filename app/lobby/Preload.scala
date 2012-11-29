package lila
package lobby

import timeline.Entry
import game.{ DbGame, Featured }
import forum.PostView
import controllers.routes
import socket.History
import tournament.Created

import play.api.mvc.Call
import play.api.libs.concurrent.Akka
import play.api.Play.current
import scala.concurrent.Future
import scala.concurrent.duration._
import akka.util.Timeout
import scalaz.effects._

final class Preload(
    fisherman: Fisherman,
    history: History,
    hookRepo: HookRepo,
    getGame: String ⇒ IO[Option[DbGame]],
    messageRepo: MessageRepo,
    featured: Featured) {

  private implicit val executor = Akka.system.dispatcher
  private implicit val timeout = Timeout(1 second)
  private type RightResponse = (Map[String, Any], List[PostView], List[Created], Option[DbGame])
  private type Response = Either[Call, RightResponse]

  def apply(
    auth: Boolean,
    chat: Boolean,
    myHook: Option[Hook],
    timeline: IO[List[Entry]],
    posts: IO[List[PostView]],
    tours: IO[List[Created]]): Future[Response] =
    myHook.flatMap(_.gameId).fold(
      futureHooks(auth) zip
        futureMessages(chat) zip
        ioToFuture(timeline) zip
        ioToFuture(posts) zip
        ioToFuture(tours) zip
        featured.one map {
          case (((((hooks, messages), entries), posts), tours), feat) ⇒ (Right((Map(
            "version" -> history.version,
            "pool" -> renderHooks(hooks, myHook),
            "chat" -> (messages.reverse map (_.render)),
            "timeline" -> (entries.reverse map (_.render))
          ), posts, tours, feat))): Response
        }) { gameId ⇒
        futureGame(gameId) map { gameOption ⇒
          Left(gameOption.fold(
            game ⇒ routes.Round.player(game fullIdOf game.creatorColor),
            routes.Lobby.home()
          )): Response
        }
      }

  private def futureHooks(auth: Boolean): Future[List[Hook]] = ioToFuture {
    auth.fold(hookRepo.allOpen, hookRepo.allOpenCasual)
  }

  private def futureMessages(chat: Boolean) = ioToFuture {
    chat.fold(messageRepo.recent, io(Nil))
  }

  private def futureGame(gameId: String): Future[Option[DbGame]] = ioToFuture {
    getGame(gameId)
  }

  private def ioToFuture[A](ioa: IO[A]): Future[A] = Future {
    ioa.unsafePerformIO
  }

  private def renderHooks(
    hooks: List[Hook],
    myHook: Option[Hook]) = hooks map { h ⇒
    myHook.exists(_.id == h.id).fold(
      h.render ++ Map("ownerId" -> h.ownerId),
      h.render)
  }
}

package lila
package lobby

import timeline.Entry
import game.DbGame
import controllers.routes

import play.api.mvc.Call
import play.api.libs.concurrent.Akka
import play.api.Play.current
import akka.dispatch.Future
import akka.util.duration._
import akka.util.Timeout
import scalaz.effects._

final class Preload(
    fisherman: Fisherman,
    history: History,
    hookRepo: HookRepo,
    getGame: String ⇒ IO[Option[DbGame]],
    messageRepo: MessageRepo) {

  private implicit val executor = Akka.system.dispatcher
  private implicit val timeout = Timeout(1 second)
  private type Response = Either[Call, Map[String, Any]]

  def apply(
    auth: Boolean,
    chat: Boolean,
    myHook: Option[Hook],
    timeline: IO[List[Entry]]): Future[Response] =
    myHook.flatMap(_.gameId).fold(
      hookBitten,
      for {
        hooks ← futureHooks(auth)
        messages ← futureMessages(chat)
        entries ← ioToFuture(timeline)
      } yield Right(Map(
        "version" -> history.version,
        "pool" -> renderHooks(hooks, myHook),
        "chat" -> (messages.reverse map (_.render)),
        "timeline" -> (entries.reverse map (_.render))
      )): Response
    )

  private def futureHooks(auth: Boolean): Future[List[Hook]] = ioToFuture {
    auth.fold(hookRepo.allOpen, hookRepo.allOpenCasual)
  }

  private def futureMessages(chat: Boolean) = ioToFuture {
    chat.fold(messageRepo.recent, io(Nil))
  }

  private def hookBitten(gameId: String): Future[Response] = ioToFuture {
    getGame(gameId) map { game ⇒
      Left(game.fold(
        g ⇒ routes.Round.player(g fullIdOf g.creatorColor),
        routes.Lobby.home()
      )): Response
    }
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

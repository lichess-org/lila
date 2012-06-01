package lila
package lobby

import timeline.Entry
import game.DbGame
import controllers.routes

import play.api.mvc.Call
import scalaz.effects._

final class Preload(
    fisherman: Fisherman,
    history: History,
    hookRepo: HookRepo,
    getGame: String ⇒ IO[Option[DbGame]],
    messageRepo: MessageRepo) {

  type Response = Either[Call, Map[String, Any]]

  def apply(
    auth: Boolean,
    chat: Boolean,
    myHook: Option[Hook],
    timeline: IO[List[Entry]]): IO[Response] = for {
    hooks ← auth.fold(hookRepo.allOpen, hookRepo.allOpenCasual)
    res ← myHook.flatMap(_.gameId).fold(
      gid ⇒ getGame(gid) map { game ⇒
        Left(game.fold(
          g ⇒ routes.Round.player(g fullIdOf g.creatorColor),
          routes.Lobby.home()
        ))
      }, for {
        messages ← chat.fold(messageRepo.recent, io(Nil))
        entries ← timeline
      } yield Right(Map(
        "version" -> history.version,
        "pool" -> renderHooks(hooks, myHook),
        "chat" -> (messages.reverse map (_.render)),
        "timeline" -> (entries.reverse map (_.render))
      ))
    )
  } yield res

  private def renderHooks(
    hooks: List[Hook],
    myHook: Option[Hook]) = hooks map { h ⇒
    myHook.exists(_.id == h.id).fold(
      h.render ++ Map("ownerId" -> h.ownerId),
      h.render)
  }
}

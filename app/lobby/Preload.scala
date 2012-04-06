package lila
package lobby

import model._
import memo._
import db._
import scalaz.effects._

final class Preload(
    fisherman: Fisherman,
    history: History,
    hookRepo: HookRepo,
    gameRepo: GameRepo,
    messageRepo: MessageRepo,
    entryRepo: EntryRepo) {

  type Response = Map[String, Any]

  def apply(
    auth: Boolean,
    chat: Boolean,
    myHookId: Option[String]): IO[Response] = for {
    myHook ← myHookId.fold(hookRepo.ownedHook, io(none))
    _ ← myHook.fold(fisherman.shake, io())
    hooks ← auth.fold(hookRepo.allOpen, hookRepo.allOpenCasual)
    res ← {
      val response = () ⇒ stdResponse(chat, hooks, myHook)
      myHook.fold(hookResponse(_, response), response())
    }
  } yield res

  private def hookResponse(
    myHook: Hook,
    response: () ⇒ IO[Response]): IO[Response] = myHook.game.fold(
      ref ⇒ gameRepo game ref.getId.toString map { game ⇒
        Map("redirect" -> (game fullIdOf game.creatorColor))
      }, response())

  private def stdResponse(
    chat: Boolean,
    hooks: List[Hook],
    myHook: Option[Hook]): IO[Response] = for {
    messages ← if (chat) messageRepo.recent else io(Nil)
    entries ← entryRepo.recent
  } yield Map(
    "version" -> history.version,
    "pool" -> renderHooks(hooks, myHook),
    "chat" -> (messages.reverse map (_.render)),
    "timeline" -> (entries.reverse map (_.render))
  )

  private def renderHooks(
    hooks: List[Hook],
    myHook: Option[Hook]) = hooks map { h ⇒
    if (myHook == Some(h))
      h.render ++ Map("action" -> "cancel", "ownerId" -> h.ownerId)
    else h.render
  }
}

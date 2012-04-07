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
    hooks ← auth.fold(hookRepo.allOpen, hookRepo.allOpenCasual)
    std = () ⇒ stdResponse(chat, hooks, myHookId)
    res ← myHookId.fold(
      id ⇒ hookRepo ownedHook id flatMap { hookResponse(_, std) },
      std()
    )
  } yield res

  private def hookResponse(
    myHook: Option[Hook],
    std: () ⇒ IO[Response]): IO[Response] = myHook.fold(
    h ⇒ h.gameId.fold(
      ref ⇒ gameRepo gameOption ref map { game ⇒
        game.fold(
          g ⇒ Map("redirect" -> (g fullIdOf g.creatorColor)),
          Map("redirect" -> "/")
        )
      },
      fisherman shake h flatMap { _ ⇒ std() }
    ),
    io(Map("redirect" -> "/"))
  )

  private def stdResponse(
    chat: Boolean,
    hooks: List[Hook],
    myHookId: Option[String]): IO[Response] = for {
    messages ← if (chat) messageRepo.recent else io(Nil)
    entries ← entryRepo.recent
  } yield Map(
    "version" -> history.version,
    "pool" -> renderHooks(hooks, myHookId),
    "chat" -> (messages.reverse map (_.render)),
    "timeline" -> (entries.reverse map (_.render))
  )

  private def renderHooks(
    hooks: List[Hook],
    myHookId: Option[String]) = hooks map { h ⇒
    if (myHookId == Some(h.ownerId)) h.render ++ Map("ownerId" -> h.ownerId)
    else h.render
  }
}

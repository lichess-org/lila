package lila.system

import model._
import memo._
import db._
import scalaz.effects._

final class LobbyPreloader(
    hookRepo: HookRepo,
    gameRepo: GameRepo,
    messageRepo: MessageRepo,
    entryRepo: EntryRepo,
    hookMemo: HookMemo) {

  type Response = Map[String, Any]

  def apply(
    auth: Boolean,
    chat: Boolean,
    myHookId: Option[String]): IO[Response] = for {
    _ ← myHookId.fold(hookMemo.put, io())
    hooks ← if (auth) hookRepo.allOpen else hookRepo.allOpenCasual
    res ← {
      val response = () ⇒ stdResponse(chat, hooks, myHookId)
      myHookId some { hookResponse(_, response) } none response()
    }
  } yield res

  def hookResponse(myHookId: String, response: () ⇒ IO[Response]): IO[Response] =
    hookRepo ownedHook myHookId flatMap { hookOption ⇒
      hookOption.fold(
        hook ⇒ hook.game.fold(
          ref ⇒ gameRepo game ref.getId.toString map { game ⇒
            Map("redirect" -> (game fullIdOf game.creatorColor))
          },
          response()
        ),
        io { Map("redirect" -> "") }
      )
    }

  def stdResponse(
    chat: Boolean,
    hooks: List[Hook],
    myHookId: Option[String]): IO[Response] = for {
    messages ← if (chat) messageRepo.recent else io(Nil)
    entries ← entryRepo.recent
  } yield Map(
    "pool" -> renderHooks(hooks, myHookId),
    "chat" -> (messages.reverse map (_.render)),
    "timeline" -> (entries.reverse map (_.render))
  )

  private def renderHooks(hooks: List[Hook], myHookId: Option[String]) = hooks map { h ⇒
    if (myHookId == Some(h.ownerId))
      h.render ++ Map("action" -> "cancel", "ownerId" -> myHookId)
    else h.render
  }
}

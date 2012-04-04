package lila.system

import model._
import memo._
import db._
import scalaz.effects._
import scalaz.NonEmptyList
import scala.annotation.tailrec
import scala.math.max

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
    "chat" -> messages.toNel.fold(renderMessages, Nil),
    "timeline" -> entries.toNel.fold(renderEntries, Nil)
  )

  private def renderMessages(messages: NonEmptyList[Message]) =
    messages.list.reverse map { message ⇒
      Map(
        "txt" -> message.text,
        "u" -> message.username)
    }

  private def renderEntries(entries: NonEmptyList[Entry]) =
    entries.list.reverse map { entry ⇒
      "<td>%s</td><td>%s</td><td class='trans_me'>%s</td><td class='trans_me'>%s</td><td class='trans_me'>%s</td>".format(
        "<a class='watch' href='/%s'></a>" format entry.data.id,
        entry.data.players map { p ⇒
          p.u.fold(
            username ⇒ "<a class='user_link' href='/@/%s'>%s</a>".format(username, p.ue),
            p.ue)
        } mkString " vs ",
        entry.data.variant,
        entry.data.rated ? "Rated" | "Casual",
        entry.data.clock | "Unlimited")
    }

  private def renderHooks(hooks: List[Hook], myHookId: Option[String]) = hooks map { h ⇒
    if (myHookId == Some(h.ownerId))
      h.render ++ Map("action" -> "cancel", "ownerId" -> myHookId)
    else h.render
  }
}

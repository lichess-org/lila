package lila.system

import model._
import memo._
import db._
import scalaz.effects._
import scalaz.NonEmptyList
import scala.annotation.tailrec
import scala.math.max
import org.apache.commons.lang3.StringEscapeUtils.escapeXml

final class LobbySyncer(
    hookRepo: HookRepo,
    gameRepo: GameRepo,
    messageRepo: MessageRepo,
    entryRepo: EntryRepo,
    lobbyMemo: LobbyMemo,
    hookMemo: HookMemo,
    messageMemo: MessageMemo,
    entryMemo: EntryMemo,
    duration: Int,
    sleep: Int) {

  type Response = Map[String, Any]

  def sync(
    myHookId: Option[String],
    auth: Boolean,
    version: Int,
    messageId: Int,
    entryId: Int): IO[Response] = for {
    _ ← myHookId.fold(hookMemo.put, io())
    newVersion ← wait(version, messageId, entryId)
    hooks ← if (auth) hookRepo.allOpen else hookRepo.allOpenCasual
    res ← {
      val response = () ⇒ stdResponse(newVersion, hooks, myHookId, messageId, entryId)
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
    version: Int,
    hooks: List[Hook],
    myHookId: Option[String],
    messageId: Int,
    entryId: Int): IO[Response] = for {
    messages ← (messageId match {
      case -1 ⇒ io(Nil)
      case 0  ⇒ messageRepo.recent
      case id ⇒ messageRepo since max(messageMemo.id - messageRepo.max, id)
    })
    entries ← if (entryId == 0) entryRepo.recent
    else entryRepo since max(entryMemo.id - entryRepo.max, entryId)
  } yield Map(
    "state" -> version,
    "pool" -> {
      if (hooks.nonEmpty) Map("hooks" -> renderHooks(hooks, myHookId).toMap)
      else Map("message" -> "No game available right now, create one!")
    },
    "chat" -> (messageId match {
      case -1 ⇒ null
      case id ⇒ messages.toNel.fold(
        renderMessages, Map("id" -> id, "messages" -> Nil))
    }),
    "timeline" -> entries.toNel.fold(
      renderEntries,
      Map("id" -> entryId, "entries" -> Nil)
    )
  )

  private def renderMessages(messages: NonEmptyList[Message]) = Map(
    "id" -> messages.head.id,
    "messages" -> (messages.list.reverse map { message ⇒
      Map(
        "id" -> message.id,
        "u" -> message.username,
        "m" -> escapeXml(message.message)
      )
    })
  )

  private def renderEntries(entries: NonEmptyList[Entry]) = Map(
    "id" -> entries.head.id,
    "entries" -> (entries.list.reverse map { entry ⇒
      "<td>%s</td><td>%s</td><td class='trans_me'>%s</td><td class='trans_me'>%s</td><td class='trans_me'>%s</td>".format(
        "<a class='watch' href='/%s'></a>" format entry.data.id,
        entry.data.players map { p ⇒
          p.u.fold(
            username ⇒ "<a class='user_link' href='/@/%s'>%s</a>".format(username, p.ue),
            p.ue)
        } mkString " vs ",
        entry.data.variant,
        entry.data.rated ? "Rated" | "Casual",
        entry.data.clock |> { c ⇒ if (c.empty) "Unlimited" else c mkString " + " }
      )
    })
  )

  private def renderHooks(hooks: List[Hook], myHookId: Option[String]) = for {
    hook ← hooks
  } yield hook.id -> {
    hook.render ++ {
      if (myHookId == Some(hook.ownerId)) Map("action" -> "cancel", "id" -> myHookId)
      else Map("action" -> "join", "id" -> hook.id)
    }
  }

  private def wait(version: Int, messageId: Int, entryId: Int): IO[Int] = io {
    @tailrec
    def wait(loop: Int): Int = {
      if (loop == 0 ||
        lobbyMemo.version != version ||
        (messageId != -1 && messageMemo.id != messageId) ||
        entryMemo.id != entryId) lobbyMemo.version
      else { Thread sleep sleep; wait(loop - 1) }
    }
    wait(max(1, duration / sleep))
  }
}

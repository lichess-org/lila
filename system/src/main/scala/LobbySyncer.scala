package lila.system

import model._
import memo._
import db._
import scalaz.effects._
import scalaz.NonEmptyList
import scala.annotation.tailrec
import scala.math.max

final class LobbySyncer(
    hookRepo: HookRepo,
    gameRepo: GameRepo,
    entryRepo: EntryRepo,
    lobbyMemo: LobbyMemo,
    hookMemo: HookMemo,
    entryMemo: EntryMemo,
    duration: Int,
    sleep: Int,
    maxEntries: Int) {

  type Response = Map[String, Any]

  def sync(
    myHookId: Option[String],
    auth: Boolean,
    version: Int,
    entryId: Int): IO[Response] = for {
    _ ← myHookId.fold(hookMemo.put, io())
    newVersion ← wait(version, entryId)
    hooks ← if (auth) hookRepo.allOpen else hookRepo.allOpenCasual
    res ← {
      val response = () ⇒ stdResponse(newVersion, hooks, myHookId, entryId)
      myHookId some { hookResponse(_, response) } none response()
    }
  } yield res

  def hookResponse(myHookId: String, response: () ⇒ IO[Response]): IO[Response] =
    hookRepo ownedHook myHookId flatMap { hookOption ⇒
      hookOption.fold(
        hook ⇒ hook.game.fold(
          ref ⇒ gameRepo game ref.getId.toString.pp map { game ⇒
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
    entryId: Int): IO[Response] = for {
    entries ←
      if (entryId == 0) entryRepo recent maxEntries
      else entryRepo since max(entryMemo.id - maxEntries, entryId)
  } yield Map(
    "state" -> version,
    "pool" -> {
      if (hooks.nonEmpty) Map("hooks" -> renderHooks(hooks, myHookId).toMap)
      else Map("message" -> "No game available right now, create one!")
    },
    "chat" -> null,
    "timeline" -> entries.toNel.fold(
      renderTimeline,
      Map("id" -> entryId, "entries" -> Nil)
    )
  )

  private def renderTimeline(entries: NonEmptyList[Entry]) = Map(
    "id" -> entries.head.id,
    "entries" -> (entries.list.reverse map { entry =>
      "<td>%s</td><td>%s</td><td class='trans_me'>%s</td><td>%s</td><td class='trans_me'>%s</td>".format(
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

  private def wait(version: Int, entryId: Int): IO[Int] = io {
    @tailrec
    def wait(loop: Int): Int = {
      if (loop == 0 ||
        lobbyMemo.version != version ||
        entryMemo.id != entryId) lobbyMemo.version
      else { Thread sleep sleep; wait(loop - 1) }
    }
    wait(max(1, duration / sleep))
  }
}

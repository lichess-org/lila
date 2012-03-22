package lila.system

import model._
import memo._
import db._
import scalaz.effects._
import scala.annotation.tailrec
import scala.math.max

final class LobbyXhr(
    hookRepo: HookRepo,
    lobbyMemo: LobbyMemo,
    duration: Int,
    sleep: Int) {

  def sync(
    myHookId: Option[String],
    auth: Boolean,
    version: Int): IO[Map[String, Any]] = for {
    newVersion ← versionWait(version)
    hooks ← if (auth) hookRepo.allOpen else hookRepo.allOpenCasual
  } yield Map(
    "state" -> newVersion,
    "pool" -> {
      if (hooks.nonEmpty) Map("hooks" -> renderHooks(hooks, myHookId).toMap)
      else Map("message" -> "No game available right now, create one!")
    },
    "chat" -> null,
    "timeline" -> ""
  )

  private def renderHooks(hooks: List[Hook], myHookId: Option[String]) = for {
    hook ← hooks
  } yield hook.id -> {
    hook.render ++ {
      if (myHookId == Some(hook.ownerId))
        Map("action" -> "cancel", "id" -> myHookId)
      else Map("action" -> "join", "id" -> hook.id)
    }
  }

  private def versionWait(version: Int): IO[Int] = io {
    @tailrec
    def wait(loop: Int): Int = {
      if (loop == 0 || lobbyMemo.version != version) lobbyMemo.version
      else { Thread sleep sleep; wait(loop - 1) }
    }
    wait(max(1, duration / sleep))
  }
}

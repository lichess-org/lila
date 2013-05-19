package lila.lobby

import actorApi._, lobby._
import tube.hookTube
import lila.db.api._
import lila.memo.ExpireSetMemo
import lila.socket.actorApi.Broom

import scala.concurrent.duration._
import akka.actor._
import akka.pattern.{ ask, pipe }

private[lobby] final class Lobby(
    biter: Biter,
    hookMemo: ExpireSetMemo,
    socket: ActorRef) extends Actor {

  def receive = {

    case GetOpen       ⇒ HookRepo.allOpen pipeTo sender
    case GetOpenCasual ⇒ HookRepo.allOpenCasual pipeTo sender

    case msg @ AddHook(hook) ⇒ blocking {
      $insert(hook) >>- (socket ! msg) >>- shake(hook)
    }

    case msg @ CancelHook(ownerId) ⇒ socket ! blocking {
      removeByOwnerId(ownerId) inject msg
    }

    case BiteHook(hookId, uid, userId, hookOwnerId) ⇒ socket ! blocking {
      (hookOwnerId ?? removeByOwnerId) >> biter(hookId, userId) map { _(uid) }
    }

    case ShakeHook(hook) ⇒ shake(hook)

    case Broom ⇒ blocking {
      HookRepo unmatchedNotInOwnerIds hookMemo.keys flatMap { hooks ⇒
        (hooks map remove).sequence.void
      }
    }
  }

  // mark the hook as active, once
  private def shake(hook: Hook) { hookMemo put hook.ownerId }

  private def removeByOwnerId(ownerId: String) =
    HookRepo ownedHook ownerId flatMap { _ ?? remove }

  private def remove(hook: Hook) =
    $remove(hook) >>-
      (socket ! RemoveHook(hook)) >>-
      (hookMemo remove hook.ownerId)

  private def blocking[A](f: Fu[A]): A = f await 1.second
}

package lila.lobby

import scala.concurrent.duration._

import actorApi._
import akka.actor._
import akka.pattern.{ ask, pipe }
import lila.db.api._
import lila.hub.actorApi.GetUids
import lila.memo.ExpireSetMemo
import lila.socket.actorApi.Broom
import makeTimeout.short

private[lobby] final class Lobby(
    biter: Biter,
    socket: ActorRef) extends Actor {

  def receive = {

    case GetOpen ⇒ sender ! HookRepo.allOpen

    case msg @ AddHook(hook) ⇒ {
      HookRepo byUid hook.uid foreach remove
      hook.sid ?? { sid ⇒ HookRepo bySid sid foreach remove }
      HookRepo save hook
      socket ! msg
    }

    case CancelHook(uid) ⇒ {
      HookRepo byUid uid foreach remove
    }

    case BiteHook(hookId, uid, userId) ⇒
      HookRepo byId hookId foreach { hook ⇒
        HookRepo.removeUids(Set(uid, hook.uid)) foreach remove
        blocking {
          biter(hook, userId) map { socket ! _(uid) } recover {
            case e: lila.common.LilaException ⇒ logwarn(e.getMessage)
          }
        }
      }

    case Broom ⇒ blocking {
      socket ? GetUids mapTo manifest[Iterable[String]] addEffect { uids ⇒
        (HookRepo openNotInUids uids.toSet) foreach remove
        HookRepo.cleanupOld foreach remove
      }
    }

    case Resync ⇒ socket ! HookIds(HookRepo.list map (_.id))
  }

  private def remove(hook: Hook) = {
    HookRepo remove hook
    socket ! RemoveHook(hook.id)
  }

  private def blocking[A](f: Fu[A]): A = f await 1.second
}

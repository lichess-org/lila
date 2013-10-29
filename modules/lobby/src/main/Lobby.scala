package lila.lobby

import scala.concurrent.duration._

import actorApi._
import akka.actor._
import akka.pattern.{ ask, pipe }
import makeTimeout.short

import lila.db.api._
import lila.hub.actorApi.GetUids
import lila.memo.ExpireSetMemo
import lila.socket.actorApi.Broom

private[lobby] final class Lobby(
    biter: Biter,
    socket: ActorRef) extends Actor {

  def receive = {

    case GetOpen ⇒ sender ! HookRepo.allOpen

    case msg @ AddHook(hook) ⇒ {
      HookRepo byUid hook.uid foreach remove
      hook.sid ?? { sid ⇒ HookRepo bySid sid foreach remove }
      HookRepo findCompatible hook find { biter.canJoin(_, hook.user) } match {
        case Some(h) ⇒ self ! BiteHook(h.id, hook.uid, hook.user map (_.id))
        case None ⇒ {
          HookRepo save hook
          socket ! msg
        }
      }
    }

    case CancelHook(uid) ⇒ {
      HookRepo byUid uid foreach remove
    }

    case BiteHook(hookId, uid, userId) ⇒ HookRepo byId hookId foreach { hook ⇒
      HookRepo byUid uid foreach remove
      biter(hook, userId) map { f ⇒
        socket ! f(uid)
        remove(hook)
      } recover {
        case e: lila.common.LilaException ⇒ logwarn(e.getMessage)
      } await 2.seconds
    }

    case Broom ⇒ socket ? GetUids mapTo manifest[Iterable[String]] foreach { uids ⇒
      val hooks = {
        (HookRepo openNotInUids uids.toSet) ::: HookRepo.cleanupOld
      }.toSet
      if (hooks.nonEmpty) self ! RemoveHooks(hooks)
    }

    case RemoveHooks(hooks) ⇒ hooks foreach remove

    case Resync             ⇒ socket ! HookIds(HookRepo.list map (_.id))
  }

  private def remove(hook: Hook) = {
    HookRepo remove hook
    socket ! RemoveHook(hook.id)
  }
}

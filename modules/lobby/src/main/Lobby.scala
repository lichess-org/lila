package lila.lobby

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.{ ask, pipe }

import actorApi._
import lila.db.api._
import lila.game.GameRepo
import lila.hub.actorApi.{ GetUids, SocketUids }
import lila.socket.actorApi.Broom
import org.joda.time.DateTime

private[lobby] final class Lobby(
    socket: ActorRef,
    seekApi: SeekApi,
    blocking: String => Fu[Set[String]],
    playban: String => Fu[Option[lila.playban.TempBan]],
    onStart: String => Unit,
    broomPeriod: FiniteDuration,
    resyncIdsPeriod: FiniteDuration) extends Actor {

  val scheduler = context.system.scheduler

  override def preStart {
    scheduler.schedule(5 seconds, broomPeriod, self, lila.socket.actorApi.Broom)
    scheduler.schedule(10 seconds, resyncIdsPeriod, self, actorApi.Resync)
  }

  def receive = {

    case HooksFor(userOption) =>
      val replyTo = sender
      (userOption.map(_.id) ?? blocking) foreach { blocks =>
        val lobbyUser = userOption map { LobbyUser.make(_, blocks) }
        replyTo ! HookRepo.vector.filter { hook =>
          ~(hook.userId |@| lobbyUser.map(_.id)).apply(_ == _) || Biter.canJoin(hook, lobbyUser)
        }
      }

    case msg@AddHook(hook) => {
      lila.mon.lobby.hook.create()
      HookRepo byUid hook.uid foreach remove
      hook.sid ?? { sid => HookRepo bySid sid foreach remove }
      findCompatible(hook) foreach {
        case Some(h) => self ! BiteHook(h.id, hook.uid, hook.user)
        case None    => self ! SaveHook(msg)
      }
    }

    case msg@AddSeek(seek) =>
      lila.mon.lobby.seek.create()
      findCompatible(seek) foreach {
        case Some(s) => self ! BiteSeek(s.id, seek.user)
        case None    => self ! SaveSeek(msg)
      }

    case SaveHook(msg) =>
      HookRepo save msg.hook
      socket ! msg

    case SaveSeek(msg) => (seekApi insert msg.seek) >>- {
      socket ! msg
    }

    case CancelHook(uid) => {
      HookRepo byUid uid foreach remove
    }

    case CancelSeek(seekId, user) => seekApi.removeBy(seekId, user.id) >>- {
      socket ! RemoveSeek(seekId)
    }

    case BiteHook(hookId, uid, user) => NoPlayban(user) {
      lila.mon.lobby.hook.join()
      HookRepo byId hookId foreach { hook =>
        HookRepo byUid uid foreach remove
        Biter(hook, uid, user) pipeTo self
      }
    }

    case BiteSeek(seekId, user) => NoPlayban(user.some) {
      lila.mon.lobby.seek.join()
      seekApi find seekId foreach {
        _ foreach { seek =>
          Biter(seek, user) pipeTo self
        }
      }
    }

    case msg@JoinHook(_, hook, game, _) =>
      onStart(game.id)
      socket ! msg
      remove(hook)

    case msg@JoinSeek(_, seek, game, _) =>
      onStart(game.id)
      socket ! msg
      seekApi.archive(seek, game.id) >>- {
        socket ! RemoveSeek(seek.id)
      }

    case Broom =>
      HookRepo.truncateIfNeeded
      implicit val timeout = makeTimeout seconds 1
      (socket ? GetUids mapTo manifest[SocketUids]).chronometer
        .logIfSlow(100, logger) { r => s"GetUids size=${r.uids.size}" }
        .mon(_.lobby.socket.getUids)
        .result
        .logFailure(logger, err => s"broom cannot get uids from socket: $err")
        .pipeTo(self)

    case SocketUids(uids) =>
      val createdBefore = DateTime.now minusSeconds 5
      val hooks = {
        (HookRepo notInUids uids).filter {
          _.createdAt isBefore createdBefore
        } ++ HookRepo.cleanupOld
      }.toSet
      // logger.debug(
      //   s"broom uids:${uids.size} before:${createdBefore} hooks:${hooks.map(_.id)}")
      if (hooks.nonEmpty) {
        // logger.debug(s"remove ${hooks.size} hooks")
        self ! RemoveHooks(hooks)
      }
      lila.mon.lobby.socket.member(uids.size)
      lila.mon.lobby.hook.size(HookRepo.size)

    case RemoveHooks(hooks) => hooks foreach remove

    case Resync =>
      socket ! HookIds(HookRepo.vector.map(_.id))
  }

  private def NoPlayban(user: Option[LobbyUser])(f: => Unit) {
    user.?? { u => playban(u.id) } foreach {
      case None => f
      case _    =>
    }
  }

  private def findCompatible(hook: Hook): Fu[Option[Hook]] =
    findCompatibleIn(hook, HookRepo findCompatible hook)

  private def findCompatibleIn(hook: Hook, in: Vector[Hook]): Fu[Option[Hook]] = in match {
    case Vector() => fuccess(none)
    case h +: rest => Biter.canJoin(h, hook.user) ?? !{
      (h.user |@| hook.user).tupled ?? {
        case (u1, u2) =>
          GameRepo.lastGameBetween(u1.id, u2.id, DateTime.now minusHours 1) map {
            _ ?? (_.aborted)
          }
      }
    } flatMap {
      case true  => fuccess(h.some)
      case false => findCompatibleIn(hook, rest)
    }
  }

  private def findCompatible(seek: Seek): Fu[Option[Seek]] =
    seekApi forUser seek.user map {
      _ find (_ compatibleWith seek)
    }

  private def remove(hook: Hook) = {
    HookRepo remove hook
    socket ! RemoveHook(hook.id)
  }
}

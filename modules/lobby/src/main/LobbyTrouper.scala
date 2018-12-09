package lila.lobby

import scala.concurrent.duration._
import scala.concurrent.Promise

import org.joda.time.DateTime

import actorApi._
import lila.game.GameRepo
import lila.hub.Trouper
import lila.socket.Socket.Uids

private[lobby] final class LobbyTrouper(
    system: akka.actor.ActorSystem,
    socket: Socket,
    seekApi: SeekApi,
    gameCache: lila.game.Cached,
    maxPlaying: Int,
    blocking: String => Fu[Set[String]],
    playban: String => Fu[Option[lila.playban.TempBan]],
    poolApi: lila.pool.PoolApi,
    onStart: lila.game.Game.ID => Unit
) extends Trouper {

  import LobbyTrouper._

  val process: Trouper.Receive = {

    case msg @ AddHook(hook) => {
      lila.mon.lobby.hook.create()
      HookRepo byUid hook.uid foreach remove
      hook.sid ?? { sid => HookRepo bySid sid foreach remove }
      (!hook.compatibleWithPools).??(findCompatible(hook)) foreach {
        case Some(h) => this ! BiteHook(h.id, hook.uid, hook.user)
        case None => this ! SaveHook(msg)
      }
    }

    case msg @ AddSeek(seek) =>
      lila.mon.lobby.seek.create()
      findCompatible(seek) foreach {
        case Some(s) => this ! BiteSeek(s.id, seek.user)
        case None => this ! SaveSeek(msg)
      }

    case SaveHook(msg) =>
      HookRepo save msg.hook
      socket ! msg

    case SaveSeek(msg) => (seekApi insert msg.seek) >>- {
      socket ! msg
    }

    case CancelHook(uid) =>
      HookRepo byUid uid foreach remove

    case CancelSeek(seekId, user) => seekApi.removeBy(seekId, user.id) >>- {
      socket ! RemoveSeek(seekId)
    }

    case BiteHook(hookId, uid, user) => NoPlayban(user) {
      HookRepo byId hookId foreach { hook =>
        HookRepo byUid uid foreach remove
        Biter(hook, uid, user) foreach this.!
      }
    }

    case BiteSeek(seekId, user) => NoPlayban(user.some) {
      gameCache.nbPlaying(user.id) foreach { nbPlaying =>
        if (nbPlaying < maxPlaying) {
          lila.mon.lobby.seek.join()
          seekApi find seekId foreach {
            _ foreach { seek =>
              Biter(seek, user) foreach this.!
            }
          }
        }
      }
    }

    case msg @ JoinHook(_, hook, game, _) =>
      onStart(game.id)
      socket ! msg
      remove(hook)

    case msg @ JoinSeek(_, seek, game, _) =>
      onStart(game.id)
      socket ! msg
      seekApi.archive(seek, game.id) >>- {
        socket ! RemoveSeek(seek.id)
      }

    case Tick(promise) =>
      HookRepo.truncateIfNeeded
      implicit val timeout = makeTimeout seconds 5
      socket.ask[Uids](GetUidsP).chronometer
        .logIfSlow(100, logger) { r => s"GetUids size=${r.uids.size}" }
        .mon(_.lobby.socket.getUids)
        .result
        .logFailure(logger, err => s"broom cannot get uids from socket: $err")
        .foreach { this ! WithPromise(_, promise) }

    case WithPromise(Uids(uids), promise) =>
      poolApi socketIds uids
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
        this ! RemoveHooks(hooks)
      }
      lila.mon.lobby.socket.member(uids.size)
      lila.mon.lobby.hook.size(HookRepo.size)
      lila.mon.trouper.queueSize("lobby")(estimateQueueSize)
      promise.success(())

    case RemoveHooks(hooks) => hooks foreach remove

    case Resync =>
      socket ! HookIds(HookRepo.vector.map(_.id))

    case msg @ HookSub(member, true) =>
      socket ! AllHooksFor(member, HookRepo.vector.filter { Biter.showHookTo(_, member) })

    case lila.pool.HookThieve.GetCandidates(clock, promise) =>
      promise success lila.pool.HookThieve.PoolHooks(HookRepo poolCandidates clock)

    case lila.pool.HookThieve.StolenHookIds(ids) =>
      HookRepo byIds ids.toSet foreach remove
  }

  private def NoPlayban(user: Option[LobbyUser])(f: => Unit): Unit = {
    user.?? { u => playban(u.id) } foreach {
      case None => f
      case _ =>
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
      case true => fuccess(h.some)
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

private object LobbyTrouper {

  private case class Tick(promise: Promise[Unit])

  private case class WithPromise[A](value: A, promise: Promise[Unit])

  def start(
    system: akka.actor.ActorSystem,
    broomPeriod: FiniteDuration,
    resyncIdsPeriod: FiniteDuration
  )(trouper: => LobbyTrouper) = {
    system.lilaBus.subscribe(trouper, 'lobbyTrouper)
    system.scheduler.schedule(15 seconds, resyncIdsPeriod)(trouper ! actorApi.Resync)
    system.scheduler.scheduleOnce(7 seconds) {
      lila.common.ResilientScheduler(
        every = broomPeriod,
        atMost = 10 seconds,
        system = system,
        logger = logger
      ) {
        trouper.ask[Unit](Tick)
      }
    }
    trouper
  }
}

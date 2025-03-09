package lila.lobby

import scalalib.actor.SyncActor

import lila.common.Bus
import lila.core.pool.{ HookThieve, IsClockCompatible }
import lila.core.socket.{ Sri, Sris }

final private class LobbySyncActor(
    seekApi: SeekApi,
    biter: Biter,
    gameApi: lila.core.game.GameApi,
    hasCurrentPlayban: lila.core.playban.HasCurrentPlayban,
    poolApi: lila.core.pool.PoolApi,
    onStart: lila.core.game.OnStart
)(using Executor, IsClockCompatible)
    extends SyncActor:

  import LobbySyncActor.*

  private val hookRepo = new HookRepo

  private var remoteDisconnectAllAt = nowInstant

  private var socket: SyncActor = new SyncActor:
    val process = { case msg =>
      println(s"stub trouper received: $msg")
    }

  val process: SyncActor.Receive =

    // solve circular reference
    case SetSocket(actor) => socket = actor

    case msg @ AddHook(hook) =>
      lila.mon.lobby.hook.create.increment()
      hookRepo.bySri(hook.sri).foreach(remove)
      hook.sid.so: sid =>
        hookRepo.bySid(sid).foreach(remove)
      (!hook.compatibleWithPools).so(findCompatible(hook)) match
        case Some(h) =>
          biteHook(h.id, hook.sri, hook.user)
          publishRemoveHook(hook)
        case None =>
          hookRepo.save(msg.hook)
          socket ! msg

    case msg @ AddSeek(seek) =>
      lila.mon.lobby.seek.create.increment()
      findCompatible(seek).foreach:
        case Some(s) => this ! BiteSeek(s.id, seek.user)
        case None    => this ! SaveSeek(msg)

    case SaveSeek(msg) =>
      seekApi.insert(msg.seek)
      socket ! msg

    case CancelHook(sri) =>
      hookRepo.bySri(sri).foreach(remove)

    case CancelSeek(seekId, user) =>
      seekApi.removeBy(seekId, user.id)
      socket ! RemoveSeek(seekId)

    case BiteHook(hookId, sri, user) =>
      NoPlayban(user):
        biteHook(hookId, sri, user)

    case BiteSeek(seekId, user) =>
      NoPlayban(user.some):
        gameApi.nbPlaying(user.id).foreach { nbPlaying =>
          if lila.core.game.maxPlaying > nbPlaying then
            lila.mon.lobby.seek.join.increment()
            seekApi
              .find(seekId)
              .foreach:
                _.foreach: seek =>
                  biter(seek, user).foreach(this.!)
        }

    case msg @ JoinHook(_, hook, game, _) =>
      onStart.exec(game.id)
      socket ! msg
      remove(hook)

    case msg @ JoinSeek(_, seek, game, _) =>
      onStart.exec(game.id)
      seekApi.archive(seek, game.id)
      socket ! msg
      socket ! RemoveSeek(seek.id)

    case LeaveAll => remoteDisconnectAllAt = nowInstant

    case Tick(promise) =>
      hookRepo.truncateIfNeeded()
      socket
        .ask[Sris](GetSrisP.apply)
        .chronometer
        .logIfSlow(100, logger): r =>
          s"GetSris size=${r.sris.size}"
        .mon(_.lobby.socket.getSris)
        .result
        .logFailure(logger, err => s"broom cannot get sris from socket: $err")
        .foreach { this ! WithPromise(_, promise) }

    case WithPromise(Sris(sris), promise) =>
      poolApi.setOnlineSris(Sris(sris))
      val fewSecondsAgo = nowInstant.minusSeconds(5)
      if remoteDisconnectAllAt.isBefore(fewSecondsAgo)
      then
        this ! RemoveHooks:
          hookRepo
            .notInSris(sris)
            .filter: h =>
              !h.boardApi && (h.createdAt.isBefore(fewSecondsAgo))
            .toSet ++ hookRepo.cleanupOld

      lila.mon.lobby.socket.member.update(sris.size)
      lila.mon.lobby.hook.size.record(hookRepo.size)
      lila.mon.actor.queueSize("lobby").update(queueSize)
      promise.success(())

    case RemoveHooks(hooks) => hooks.foreach(remove)

    case Resync => socket ! HookIds(hookRepo.ids)

    case HookSub(member, true) =>
      socket ! AllHooksFor(member, hookRepo.filter { biter.showHookTo(_, member) }.toSeq)

    case HookThieve.GetCandidates(clock, promise) =>
      promise.success(HookThieve.PoolHooks(hookRepo.poolCandidates(clock)))

    case HookThieve.StolenHookIds(ids) =>
      hookRepo.byIds(ids.toSet).foreach(remove)

  private def NoPlayban(user: Option[LobbyUser])(f: => Unit): Unit =
    user
      .so(u => hasCurrentPlayban(u.id))
      .foreach:
        if _ then () else f

  private def biteHook(hookId: String, sri: Sri, user: Option[LobbyUser]) =
    hookRepo.byId(hookId).foreach { hook =>
      remove(hook)
      hookRepo.bySri(sri).foreach(remove)
      biter(hook, sri, user).foreach(this.!)
    }

  private def findCompatible(hook: Hook): Option[Hook] =
    hookRepo.filter(_.compatibleWith(hook)).find { existing =>
      biter.canJoin(existing, hook.user) &&
      !(existing.user, hook.user).tupled
        .so: (u1, u2) =>
          recentlyAbortedUserIdPairs.exists(u1.id, u2.id)
    }

  def registerAbortedGame(g: Game) = recentlyAbortedUserIdPairs.register(g)

  private object recentlyAbortedUserIdPairs:
    private val cache = scalalib.cache.ExpireSetMemo[String](1.hour)
    private def makeKey(u1: UserId, u2: UserId) = String:
      if u1.value < u2.value then s"$u1/$u2" else s"$u2/$u1"

    def register(g: Game) =
      for
        w <- g.whitePlayer.userId
        b <- g.blackPlayer.userId
        if g.sourceIs(_.Lobby)
      do cache.put(makeKey(w, b))
    def exists(u1: UserId, u2: UserId) = cache.get(makeKey(u1, u2))

  private def findCompatible(seek: Seek): Fu[Option[Seek]] =
    seekApi
      .forUser(seek.user)
      .map:
        _.find(_.compatibleWith(seek))

  private def remove(hook: Hook) =
    if hookRepo.exists(hook) then
      hookRepo.remove(hook)
      socket ! RemoveHook(hook.id)
      publishRemoveHook(hook)

  private def publishRemoveHook(hook: Hook): Unit =
    Bus.publish(RemoveHook(hook.id), s"hookRemove:${hook.id}")

private object LobbySyncActor:

  case class SetSocket(actor: SyncActor)

  private case class Tick(promise: Promise[Unit])

  private case class WithPromise[A](value: A, promise: Promise[Unit])

  def start(
      broomPeriod: FiniteDuration,
      resyncIdsPeriod: FiniteDuration
  )(makeActor: () => LobbySyncActor)(using ec: Executor, scheduler: Scheduler) =
    val actor = makeActor()
    Bus.subscribe(actor, "lobbyActor")
    scheduler.scheduleWithFixedDelay(15.seconds, resyncIdsPeriod)(() => actor ! Resync)
    lila.common.LilaScheduler(
      "LobbySyncActor",
      _.Every(broomPeriod),
      _.AtMost(10.seconds),
      _.Delay(7.seconds)
    ):
      actor.ask[Unit](Tick.apply)
    actor

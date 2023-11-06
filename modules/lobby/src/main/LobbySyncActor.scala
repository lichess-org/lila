package lila.lobby

import actorApi.*

import lila.common.{ Bus, LilaScheduler }
import lila.game.Game
import lila.hub.SyncActor
import lila.socket.Socket.{ Sri, Sris }

final private class LobbySyncActor(
    seekApi: SeekApi,
    biter: Biter,
    gameCache: lila.game.Cached,
    playbanApi: lila.playban.PlaybanApi,
    poolApi: lila.pool.PoolApi,
    onStart: lila.round.OnStart
)(using Executor)
    extends SyncActor:

  import LobbySyncActor.*

  private val hookRepo = new HookRepo

  private var remoteDisconnectAllAt = nowInstant

  private var socket: SyncActor = SyncActor.stub

  val process: SyncActor.Receive =

    // solve circular reference
    case SetSocket(actor) => socket = actor

    case msg @ AddHook(hook) =>
      lila.mon.lobby.hook.create.increment()
      hookRepo bySri hook.sri foreach remove
      hook.sid.so: sid =>
        hookRepo bySid sid foreach remove
      !hook.compatibleWithPools so findCompatible(hook) match
        case Some(h) =>
          biteHook(h.id, hook.sri, hook.user)
          publishRemoveHook(hook)
        case None =>
          hookRepo save msg.hook
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
      hookRepo bySri sri foreach remove

    case CancelSeek(seekId, user) =>
      seekApi.removeBy(seekId, user.id)
      socket ! RemoveSeek(seekId)

    case BiteHook(hookId, sri, user) =>
      NoPlayban(user):
        biteHook(hookId, sri, user)

    case BiteSeek(seekId, user) =>
      NoPlayban(user.some):
        gameCache.nbPlaying(user.id) foreach { nbPlaying =>
          if lila.game.Game.maxPlaying > nbPlaying then
            lila.mon.lobby.seek.join.increment()
            seekApi find seekId foreach {
              _.foreach: seek =>
                biter(seek, user) foreach this.!
            }
        }

    case msg @ JoinHook(_, hook, game, _) =>
      onStart(game.id)
      socket ! msg
      remove(hook)

    case msg @ JoinSeek(_, seek, game, _) =>
      onStart(game.id)
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
      poolApi socketIds Sris(sris)
      val fewSecondsAgo = nowInstant minusSeconds 5
      if remoteDisconnectAllAt isBefore fewSecondsAgo
      then
        this ! RemoveHooks:
          hookRepo
            .notInSris(sris)
            .filter: h =>
              !h.boardApi && (h.createdAt isBefore fewSecondsAgo)
            .toSet ++ hookRepo.cleanupOld

      lila.mon.lobby.socket.member.update(sris.size)
      lila.mon.lobby.hook.size.record(hookRepo.size)
      lila.mon.actor.queueSize("lobby").update(queueSize)
      promise.success(())

    case RemoveHooks(hooks) => hooks foreach remove

    case Resync => socket ! HookIds(hookRepo.ids)

    case HookSub(member, true) =>
      socket ! AllHooksFor(member, hookRepo.filter { biter.showHookTo(_, member) }.toSeq)

    case lila.pool.HookThieve.GetCandidates(clock, promise) =>
      promise success lila.pool.HookThieve.PoolHooks(hookRepo poolCandidates clock)

    case lila.pool.HookThieve.StolenHookIds(ids) =>
      hookRepo byIds ids.toSet foreach remove

  private def NoPlayban(user: Option[LobbyUser])(f: => Unit): Unit =
    user
      .so(playbanApi.currentBan)
      .foreach:
        case None => f
        case _    =>

  private def biteHook(hookId: String, sri: Sri, user: Option[LobbyUser]) =
    hookRepo byId hookId foreach { hook =>
      remove(hook)
      hookRepo bySri sri foreach remove
      biter(hook, sri, user) foreach this.!
    }

  private def findCompatible(hook: Hook): Option[Hook] =
    hookRepo.filter(_ compatibleWith hook).find { existing =>
      biter.canJoin(existing, hook.user) &&
      !(existing.user, hook.user).tupled
        .so: (u1, u2) =>
          recentlyAbortedUserIdPairs.exists(u1.id, u2.id)
    }

  def registerAbortedGame(g: Game) = recentlyAbortedUserIdPairs register g

  private object recentlyAbortedUserIdPairs:
    private val cache = lila.memo.ExpireSetMemo[CacheKey](1 hour)
    private def makeKey(u1: UserId, u2: UserId) = CacheKey:
      if u1.value < u2.value then s"$u1/$u2" else s"$u2/$u1"

    def register(g: Game) =
      for
        w <- g.whitePlayer.userId
        b <- g.blackPlayer.userId
        if g.fromLobby
      do cache.put(makeKey(w, b))
    def exists(u1: UserId, u2: UserId) = cache.get(makeKey(u1, u2))

  private def findCompatible(seek: Seek): Fu[Option[Seek]] =
    seekApi forUser seek.user map {
      _.find(_ compatibleWith seek)
    }

  private def remove(hook: Hook) =
    if hookRepo.exists(hook) then
      hookRepo remove hook
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
    scheduler.scheduleWithFixedDelay(15 seconds, resyncIdsPeriod)(() => actor ! actorApi.Resync)
    lila.common.LilaScheduler(
      "LobbySyncActor",
      _.Every(broomPeriod),
      _.AtMost(10 seconds),
      _.Delay(7 seconds)
    ):
      actor.ask[Unit](Tick.apply)
    actor

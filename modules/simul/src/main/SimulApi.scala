package lila.simul

import akka.actor.*
import chess.variant.Variant
import chess.{ ByColor, Status }
import monocle.syntax.all.*
import play.api.libs.json.Json
import scalalib.paginator.Paginator
import scalalib.Debouncer

import lila.common.Json.given
import lila.common.Bus
import lila.core.perf.UserWithPerfs
import lila.core.socket.SendToFlag
import lila.core.team.LightTeam
import lila.core.timeline.{ Propagate, SimulCreate, SimulJoin }
import lila.db.dsl.{ *, given }
import lila.gathering.Condition
import lila.gathering.Condition.GetMyTeamIds
import lila.memo.CacheApi.*
import lila.rating.PerfType
import lila.rating.UserWithPerfs.only

final class SimulApi(
    userApi: lila.core.user.UserApi,
    gameRepo: lila.core.game.GameRepo,
    newPlayer: lila.core.game.NewPlayer,
    onGameStart: lila.core.game.OnStart,
    socket: SimulSocket,
    repo: SimulRepo,
    verify: SimulCondition.Verify,
    cacheApi: lila.memo.CacheApi
)(using Executor)(using scheduler: Scheduler)
    extends lila.core.simul.SimulApi:

  private val workQueue = scalalib.actor.AsyncActorSequencers[SimulId](
    maxSize = Max(128),
    expiration = 10.minutes,
    timeout = 10.seconds,
    name = "simulApi",
    lila.log.asyncActorMonitor.full
  )

  export repo.{ find, byIds, byTeamLeaders }

  def currentHostIds: Fu[Set[UserId]] = currentHostIdsCache.get {}

  def isSimulHost(userId: UserId): Fu[Boolean] = currentHostIds.map(_ contains userId)

  private val currentHostIdsCache = cacheApi.unit[Set[UserId]]:
    _.refreshAfterWrite(5.minutes).buildAsyncFuture: _ =>
      repo.allStarted.dmap(_.view.map(_.hostId).toSet)

  def create(setup: SimulForm.Setup, teams: Seq[LightTeam])(using me: Me): Fu[Simul] = for
    host <- userApi.withPerfs(me.value)
    simul = Simul.make(
      name = setup.name,
      clock = setup.clock,
      variants = setup.actualVariants,
      position = setup.realPosition,
      host = host,
      color = setup.color,
      text = setup.text,
      estimatedStartAt = setup.estimatedStartAt,
      featurable = some(~setup.featured && canBeFeatured(me)),
      conditions = setup.conditions
    )
    _ <- repo.create(simul)
  yield
    publish()
    Bus.pub(Propagate(SimulCreate(me.userId, simul.id, simul.fullName)).toFollowersOf(me.userId))
    simul

  def update(prev: Simul, setup: SimulForm.Setup, teams: Seq[LightTeam])(using me: Me): Fu[Simul] =
    val simul = prev.copy(
      name = setup.name,
      clock = setup.clock,
      variants = setup.actualVariants,
      applicants = prev.applicants.filter(setup.actualVariants contains _.player.variant),
      position = setup.realPosition,
      color = setup.color.some,
      text = setup.text,
      estimatedStartAt = setup.estimatedStartAt,
      featurable = some(~setup.featured && canBeFeatured(me)),
      conditions = setup.conditions
    )
    update(simul).inject(simul)

  def update(prev: Simul, setup: SimulForm.LockedSetup): Fu[Simul] =
    val simul = prev.copy(
      name = setup.name,
      text = setup.text
    )
    update(simul).inject(simul)

  def getVerdicts(simul: Simul)(using
      me: Option[Me]
  )(using GetMyTeamIds, Perf): Fu[Condition.WithVerdicts] =
    me.foldUse(fuccess(simul.conditions.accepted)):
      verify(simul, simul.mainPerfType)

  def addApplicant(simulId: SimulId, variantKey: Variant.LilaKey)(using me: Me)(using GetMyTeamIds): Funit =
    workQueue(simulId):
      repo
        .findCreated(simulId)
        .flatMapz: simul =>
          Variant(variantKey)
            .filter(simul.variants.contains)
            .ifTrue(simul.nbAccepted < lila.core.game.maxPlayingRealtime.value)
            .so: variant =>
              val perfType = PerfType(variant, chess.Speed(simul.clock.config.some))
              userApi
                .withPerf(me.value, perfType)
                .flatMap: user =>
                  given Perf = user.perf
                  verify(simul, perfType).flatMap:
                    _.accepted.so:
                      val player   = SimulPlayer.make(user, variant)
                      val newSimul = simul.addApplicant(SimulApplicant(player, accepted = false))
                      for _ <- repo.update(newSimul)
                      yield
                        Bus.pub:
                          Propagate(SimulJoin(me.userId, simul.id, simul.fullName)).toFollowersOf(user.id)
                        socket.reload(newSimul.id)
                        publish()

  def removeApplicant(simulId: SimulId, user: User): Funit =
    UpdateCreatedSimul(simulId) { _.removeApplicant(user.id) }

  def accept(simulId: SimulId, userId: UserId, v: Boolean): Funit =
    userApi.byId(userId).flatMapz { user =>
      UpdateCreatedSimul(simulId) { _.accept(user.id, v) }
    }

  def start(simulId: SimulId): Funit =
    workQueue(simulId):
      repo.findCreated(simulId).flatMapz { simul =>
        simul.start.so: started =>
          userApi
            .withPerfs(started.hostId)
            .orFail(s"No such host: ${simul.hostId}")
            .flatMap: host =>
              started.pairings.mapWithIndex(makeGame(started, host)).parallel.map { games =>
                games.headOption.foreach: (game, _) =>
                  socket.startSimul(simul, game)
                games.foldLeft(started):
                  case (s, (g, hostColor)) => s.setPairingHostColor(g.id, hostColor)
              }
            .flatMap: s =>
              Bus.publish(lila.core.simul.OnStart(s), "startSimul")
              for _ <- update(s) yield currentHostIdsCache.invalidateUnit()
      }

  def onPlayerConnection(game: Game, user: Option[User])(simul: Simul): Unit =
    if user.exists(simul.isHost) && simul.isRunning then
      repo.setHostGameId(simul, game.id)
      socket.hostIsOn(simul.id, game.id)

  def abort(simulId: SimulId): Funit =
    workQueue(simulId):
      repo.findCreated(simulId).flatMapz { simul =>
        for _ <- repo.remove(simul)
        yield
          socket.aborted(simul.id)
          publish()
      }

  def setText(simulId: SimulId, text: String): Funit =
    workQueue(simulId):
      repo.find(simulId).flatMapz { simul =>
        for _ <- repo.setText(simul, text) yield socket.reload(simulId)
      }

  private[simul] def finishGame(game: Game): Funit =
    game.simulId.so:
      finishGame(_, game.id, game.status, game.winnerUserId)

  private def finishGame(simulId: SimulId, gameId: GameId, status: Status, winner: Option[UserId]): Funit =
    workQueue(simulId):
      repo
        .findStarted(simulId)
        .flatMapz: simul =>
          val simul2 = simul.updatePairing(gameId, _.finish(status, winner))
          for _ <- update(simul2)
          yield if simul2.isFinished then onComplete(simul2)

  private def onComplete(simul: Simul): Unit =
    currentHostIdsCache.invalidateUnit()
    Bus.publish(
      lila.core.socket.SendTo(
        simul.hostId,
        lila.core.socket.makeMessage(
          "simulEnd",
          Json.obj(
            "id"   -> simul.id,
            "name" -> simul.name
          )
        )
      ),
      "socketUsers"
    )

  def ejectCheater(userId: UserId): Unit =
    repo.allNotFinished.foreach:
      _.foreach: oldSimul =>
        workQueue(oldSimul.id):
          repo.findCreated(oldSimul.id).flatMapz { simul =>
            simul.ejectCheater(userId).so { simul2 =>
              update(simul2).void
            }
          }

  def hostPing(simul: Simul): Funit =
    simul.isCreated.so:
      for
        _ <- repo.setHostSeenNow(simul)
        applicantIds = simul.applicants.view.map(_.player.user).toSet
        online <- socket.filterPresent(simul, applicantIds)
        leaving = applicantIds.diff(online.toSet)
        _ <- leaving.nonEmpty.so:
          UpdateCreatedSimul(simul.id):
            _.copy(applicants = simul.applicants.filterNot(a => leaving(a.player.user)))
      yield ()

  def idToName(id: SimulId): Fu[Option[String]] =
    repo.coll.primitiveOne[String]($id(id), "name").dmap2(_ + " simul")

  def teamOf(id: SimulId): Fu[Option[TeamId]] =
    repo.coll.primitiveOne[TeamId]($id(id), "team")

  def hostedByUser(userId: UserId, page: Int): Fu[Paginator[Simul]] =
    Paginator(
      adapter = repo.byHostAdapter(userId),
      currentPage = page,
      maxPerPage = MaxPerPage(20)
    )

  object countHostedByUser:
    private val cache = cacheApi[UserId, Int](32_768, "simul.nb.hosted"):
      _.expireAfterWrite(5.minutes).buildAsyncFuture(repo.countByHost)
    export cache.get

  private def makeGame(simul: Simul, host: UserWithPerfs)(
      pairing: SimulPairing,
      number: Int
  ): Fu[(Game, Color)] = for
    user <- userApi.withPerfs(pairing.player.user).orFail(s"No user with id ${pairing.player.user}")
    hostColor = simul.hostColor | Color.fromWhite(number % 2 == 0)
    us        = ByColor(host, user)
    users     = hostColor.fold(us, us.swap)
    clock     = simul.clock.chessClockOf(hostColor)
    perfType  = PerfType(pairing.player.variant, chess.Speed(clock.config))
    game1 = lila.core.game.newGame(
      chess = chess
        .Game(
          variantOption = Some:
            if simul.position.isEmpty
            then pairing.player.variant
            else chess.variant.FromPosition
          ,
          fen = simul.position
        )
        .copy(clock = clock.start.some),
      players = users.mapWithColor((c, u) => newPlayer(c, u.only(perfType).some)),
      mode = chess.Mode.Casual,
      source = lila.core.game.Source.Simul,
      pgnImport = None
    )
    game2 = game1
      .withId(pairing.gameId)
      .focus(_.metadata.simulId)
      .replace(simul.id.some)
      .start
    _ <- gameRepo.insertDenormalized(game2)
  yield
    onGameStart.exec(game2.id)
    socket.startGame(simul, game2)
    game2 -> hostColor

  // clean up unfinished simuls
  // by making sure ongoing games really are still being played
  def checkOngoingSimuls(simuls: List[Simul]): Funit =
    val sampling = 100
    simuls
      .filter(_.startedAt.exists(_.isBefore(nowInstant.minusHours(1))))
      // only test some random simuls, no need to test all of them all the time
      .filter(_.startedAt.exists(_.getEpochSecond % sampling == (nowSeconds % sampling)))
      .sequentiallyVoid: simul =>
        gameRepo.light
          .gamesFromPrimary(simul.ongoingGameIds)
          .flatMap: games =>
            val dirty: List[(GameId, Status, Option[UserId])] = simul.ongoingGameIds.flatMap: gameId =>
              games.find(_.id == gameId) match
                case None => (gameId, Status.UnknownFinish, none).some // the game is not in DB!!
                case Some(g) if g.status >= Status.Aborted =>
                  (gameId, g.status, g.winnerUserId).some // DB game is finished
                case _ => none
            dirty.sequentiallyVoid: (id, status, winner) =>
              logger.info(s"Simul ${simul.id} game $id is dirty, finishing with $status")
              finishGame(simul.id, id, status, winner)

  private def update(simul: Simul): Funit =
    for _ <- repo.update(simul)
    yield
      socket.reload(simul.id)
      publish()

  private def UpdateCreatedSimul(simulId: SimulId)(updating: Simul => Simul): Funit =
    workQueue(simulId):
      repo
        .findCreated(simulId)
        .flatMapz: simul =>
          update(updating(simul))

  private object publish:
    private val siteMessage = SendToFlag("simul", Json.obj("t" -> "reload"))
    private val debouncer =
      Debouncer[Unit](scheduler.scheduleOnce(5.seconds, _), 1): _ =>
        Bus.publish(siteMessage, "sendToFlag")
    def apply() = debouncer.push(())

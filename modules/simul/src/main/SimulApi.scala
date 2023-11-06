package lila.simul

import akka.actor.*
import chess.variant.Variant
import chess.ByColor
import play.api.libs.json.Json

import lila.common.{ Bus, Debouncer }
import lila.db.dsl.{ *, given }
import lila.game.{ Game, GameRepo }
import lila.hub.actorApi.timeline.{ Propagate, SimulCreate, SimulJoin }
import lila.memo.CacheApi.*
import lila.socket.SendToFlag
import lila.user.{ User, Me, UserRepo, UserPerfsRepo, UserApi, UserPerfs }
import lila.common.Json.given
import lila.hub.LightTeam
import lila.gathering.Condition
import lila.gathering.Condition.GetMyTeamIds
import lila.rating.{ Perf, PerfType }
import lila.common.paginator.Paginator
import lila.common.config.{ Max, MaxPerPage }

final class SimulApi(
    userRepo: UserRepo,
    perfsRepo: UserPerfsRepo,
    userApi: UserApi,
    gameRepo: GameRepo,
    onGameStart: lila.round.OnStart,
    socket: SimulSocket,
    timeline: lila.hub.actors.Timeline,
    repo: SimulRepo,
    verify: SimulCondition.Verify,
    cacheApi: lila.memo.CacheApi
)(using Executor, Scheduler):

  private val workQueue = lila.hub.AsyncActorSequencers[SimulId](
    maxSize = Max(128),
    expiration = 10 minutes,
    timeout = 10 seconds,
    name = "simulApi"
  )

  def currentHostIds: Fu[Set[UserId]] = currentHostIdsCache.get {}

  export repo.{ find, byIds, byTeamLeaders }

  private val currentHostIdsCache = cacheApi.unit[Set[UserId]]:
    _.refreshAfterWrite(5 minutes).buildAsyncFuture: _ =>
      repo.allStarted dmap (_.view.map(_.hostId).toSet)

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
      featurable = some(~setup.featured && me.canBeFeatured),
      conditions = setup.conditions
    )
    _ <- repo.create(simul)
  yield
    publish()
    timeline ! (Propagate(SimulCreate(me.userId, simul.id, simul.fullName)) toFollowersOf me.userId)
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
      featurable = some(~setup.featured && me.canBeFeatured),
      conditions = setup.conditions
    )
    repo.update(simul) andDo publish() inject simul

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
            .ifTrue(simul.nbAccepted < Game.maxPlayingRealtime)
            .so: variant =>
              val perfType = PerfType(variant, chess.Speed(simul.clock.config.some))
              perfsRepo
                .withPerf(me.value, perfType)
                .flatMap: user =>
                  given Perf = user.perf
                  verify(simul, perfType).flatMap:
                    _.accepted.so:
                      val player   = SimulPlayer.make(user, variant)
                      val newSimul = simul addApplicant SimulApplicant(player, accepted = false)
                      repo.update(newSimul) andDo {
                        timeline ! Propagate(SimulJoin(me.userId, simul.id, simul.fullName))
                          .toFollowersOf(user.id)
                        socket.reload(newSimul.id)
                        publish()
                      }

  def removeApplicant(simulId: SimulId, user: User): Funit =
    WithSimul(repo.findCreated, simulId) { _ removeApplicant user.id }

  def accept(simulId: SimulId, userId: UserId, v: Boolean): Funit =
    userRepo byId userId flatMapz { user =>
      WithSimul(repo.findCreated, simulId) { _.accept(user.id, v) }
    }

  def start(simulId: SimulId): Funit =
    workQueue(simulId):
      repo.findCreated(simulId) flatMapz { simul =>
        simul.start.so: started =>
          userApi withPerfs started.hostId orFail s"No such host: ${simul.hostId}" flatMap { host =>
            started.pairings.mapWithIndex(makeGame(started, host)).parallel map { games =>
              games.headOption.foreach: (game, _) =>
                socket.startSimul(simul, game)
              games.foldLeft(started):
                case (s, (g, hostColor)) => s.setPairingHostColor(g.id, hostColor)
            }
          } flatMap { s =>
            Bus.publish(Simul.OnStart(s), "startSimul")
            update(s) andDo currentHostIdsCache.invalidateUnit()
          }
      }

  def onPlayerConnection(game: Game, user: Option[User])(simul: Simul): Unit =
    if user.exists(simul.isHost) && simul.isRunning then
      repo.setHostGameId(simul, game.id)
      socket.hostIsOn(simul.id, game.id)

  def abort(simulId: SimulId): Funit =
    workQueue(simulId):
      repo.findCreated(simulId) flatMapz { simul =>
        (repo remove simul) andDo socket.aborted(simul.id) andDo publish()
      }

  def setText(simulId: SimulId, text: String): Funit =
    workQueue(simulId):
      repo.find(simulId) flatMapz { simul =>
        repo.setText(simul, text) andDo socket.reload(simulId)
      }

  def finishGame(game: Game): Funit =
    game.simulId.so: simulId =>
      workQueue(simulId):
        repo.findStarted(simulId) flatMapz { simul =>
          val simul2 = simul.updatePairing(
            game.id,
            _.finish(game.status, game.winnerUserId)
          )
          update(simul2).andDo:
            if simul2.isFinished then onComplete(simul2)
        }

  private def onComplete(simul: Simul): Unit =
    currentHostIdsCache.invalidateUnit()
    Bus.publish(
      lila.hub.actorApi.socket.SendTo(
        simul.hostId,
        lila.socket.Socket.makeMessage(
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
      repo.setHostSeenNow(simul) >> {
        val applicantIds = simul.applicants.view.map(_.player.user).toSet
        socket.filterPresent(simul, applicantIds) flatMap { online =>
          val leaving = applicantIds diff online.toSet
          leaving.nonEmpty so
            WithSimul(repo.findCreated, simul.id):
              _.copy(applicants = simul.applicants.filterNot(a => leaving(a.player.user)))
        }
      }

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

  private def makeGame(simul: Simul, host: User.WithPerfs)(
      pairing: SimulPairing,
      number: Int
  ): Fu[(Game, chess.Color)] = for
    user <- userApi withPerfs pairing.player.user orFail s"No user with id ${pairing.player.user}"
    hostColor = simul.hostColor | chess.Color.fromWhite(number % 2 == 0)
    us        = ByColor(host, user)
    users     = hostColor.fold(us, us.swap)
    clock     = simul.clock.chessClockOf(hostColor)
    perfType  = PerfType(pairing.player.variant, chess.Speed(clock.config))
    game1 = Game.make(
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
      players = users.mapWithColor((c, u) => lila.game.Player.make(c, u.only(perfType).some)),
      mode = chess.Mode.Casual,
      source = lila.game.Source.Simul,
      pgnImport = None
    )
    game2 = game1
      .withId(pairing.gameId)
      .withSimulId(simul.id)
      .start
    _ <- gameRepo.insertDenormalized(game2)
  yield
    onGameStart(game2.id)
    socket.startGame(simul, game2)
    game2 -> hostColor

  private def update(simul: Simul): Funit =
    repo.update(simul) andDo socket.reload(simul.id) andDo publish()

  private def WithSimul(
      finding: SimulId => Fu[Option[Simul]],
      simulId: SimulId
  )(updating: Simul => Simul): Funit =
    workQueue(simulId):
      finding(simulId).flatMapz: simul =>
        update(updating(simul))

  private object publish:
    private val siteMessage = SendToFlag("simul", Json.obj("t" -> "reload"))
    private val debouncer   = Debouncer[Unit](5 seconds, 1)(_ => Bus.publish(siteMessage, "sendToFlag"))
    def apply()             = debouncer.push(())

package lila.simul

import akka.actor.*
import chess.variant.Variant
import play.api.libs.json.Json

import lila.common.{ Bus, Debouncer }
import lila.db.dsl.{ *, given }
import lila.game.{ Game, GameRepo, PerfPicker }
import lila.hub.actorApi.timeline.{ Propagate, SimulCreate, SimulJoin }
import lila.memo.CacheApi.*
import lila.socket.SendToFlag
import lila.user.{ User, UserRepo }
import lila.common.config.Max
import lila.common.Json.given

final class SimulApi(
    userRepo: UserRepo,
    gameRepo: GameRepo,
    onGameStart: lila.round.OnStart,
    socket: SimulSocket,
    timeline: lila.hub.actors.Timeline,
    repo: SimulRepo,
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

  private val currentHostIdsCache = cacheApi.unit[Set[UserId]] {
    _.refreshAfterWrite(5 minutes)
      .buildAsyncFuture { _ =>
        repo.allStarted dmap (_.view.map(_.hostId).toSet)
      }
  }

  def create(setup: SimulForm.Setup, me: User): Fu[Simul] =
    val simul = Simul.make(
      name = setup.name,
      clock = setup.clock,
      variants = setup.actualVariants,
      position = setup.realPosition,
      host = me,
      color = setup.color,
      text = setup.text,
      estimatedStartAt = setup.estimatedStartAt,
      team = setup.team,
      featurable = some(~setup.featured && me.canBeFeatured)
    )
    repo.create(simul) >>- publish() >>- {
      timeline ! (Propagate(SimulCreate(me.id, simul.id, simul.fullName)) toFollowersOf me.id)
    } inject simul

  def update(prev: Simul, setup: SimulForm.Setup, me: User): Fu[Simul] =
    val simul = prev.copy(
      name = setup.name,
      clock = setup.clock,
      variants = setup.actualVariants,
      position = setup.realPosition,
      color = setup.color.some,
      text = setup.text,
      estimatedStartAt = setup.estimatedStartAt,
      team = setup.team,
      featurable = some(~setup.featured && me.canBeFeatured)
    )
    repo.update(simul) >>- publish() inject simul

  def addApplicant(
      simulId: SimulId,
      user: User,
      isInTeam: TeamId => Boolean,
      variantKey: Variant.LilaKey
  ): Funit =
    WithSimul(repo.findCreated, simulId) { simul =>
      if (simul.nbAccepted < Game.maxPlayingRealtime && simul.team.forall(isInTeam))
        timeline ! (Propagate(SimulJoin(user.id, simul.id, simul.fullName)) toFollowersOf user.id)
        Variant(variantKey).filter(simul.variants.contains).fold(simul) { variant =>
          simul addApplicant SimulApplicant.make(
            SimulPlayer.make(
              user,
              variant,
              PerfPicker.mainOrDefault(
                speed = chess.Speed(simul.clock.config.some),
                variant = variant,
                daysPerTurn = none
              )(user.perfs)
            )
          )
        }
      else simul
    }

  def removeApplicant(simulId: SimulId, user: User): Funit =
    WithSimul(repo.findCreated, simulId) { _ removeApplicant user.id }

  def accept(simulId: SimulId, userId: UserId, v: Boolean): Funit =
    userRepo byId userId flatMapz { user =>
      WithSimul(repo.findCreated, simulId) { _.accept(user.id, v) }
    }

  def start(simulId: SimulId): Funit =
    workQueue(simulId) {
      repo.findCreated(simulId) flatMapz { simul =>
        simul.start ?? { started =>
          userRepo byId started.hostId orFail s"No such host: ${simul.hostId}" flatMap { host =>
            started.pairings.zipWithIndex.map(makeGame(started, host)).parallel map { games =>
              games.headOption foreach { case (game, _) =>
                socket.startSimul(simul, game)
              }
              games.foldLeft(started) { case (s, (g, hostColor)) =>
                s.setPairingHostColor(g.id, hostColor)
              }
            }
          } flatMap { s =>
            Bus.publish(Simul.OnStart(s), "startSimul")
            update(s) >>- currentHostIdsCache.invalidateUnit()
          }
        }
      }
    }

  def onPlayerConnection(game: Game, user: Option[User])(simul: Simul): Unit =
    if (user.exists(simul.isHost) && simul.isRunning)
      repo.setHostGameId(simul, game.id)
      socket.hostIsOn(simul.id, game.id)

  def abort(simulId: SimulId): Funit =
    workQueue(simulId) {
      repo.findCreated(simulId) flatMapz { simul =>
        (repo remove simul) >>- socket.aborted(simul.id) >>- publish()
      }
    }

  def setText(simulId: SimulId, text: String): Funit =
    workQueue(simulId) {
      repo.find(simulId) flatMapz { simul =>
        repo.setText(simul, text) >>- socket.reload(simulId)
      }
    }

  def finishGame(game: Game): Funit =
    game.simulId ?? { simulId =>
      workQueue(simulId) {
        repo.findStarted(simulId) flatMapz { simul =>
          val simul2 = simul.updatePairing(
            game.id,
            _.finish(game.status, game.winnerUserId)
          )
          update(simul2) >>- {
            if (simul2.isFinished) onComplete(simul2)
          }
        }
      }
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
    repo.allNotFinished foreach {
      _ foreach { oldSimul =>
        workQueue(oldSimul.id) {
          repo.findCreated(oldSimul.id) flatMapz { simul =>
            (simul ejectCheater userId) ?? { simul2 =>
              update(simul2).void
            }
          }
        }
      }
    }

  def hostPing(simul: Simul): Funit =
    simul.isCreated ?? {
      repo.setHostSeenNow(simul) >> {
        val applicantIds = simul.applicants.view.map(_.player.user).toSet
        socket.filterPresent(simul, applicantIds) flatMap { online =>
          val leaving = applicantIds diff online.toSet
          leaving.nonEmpty ??
            WithSimul(repo.findCreated, simul.id) {
              _.copy(applicants = simul.applicants.filterNot(a => leaving(a.player.user)))
            }
        }
      }
    }

  def idToName(id: SimulId): Fu[Option[String]] =
    repo.coll.primitiveOne[String]($id(id), "name").dmap2(_ + " simul")

  def teamOf(id: SimulId): Fu[Option[TeamId]] =
    repo.coll.primitiveOne[TeamId]($id(id), "team")

  private def makeGame(simul: Simul, host: User)(
      pairingAndNumber: (SimulPairing, Int)
  ): Fu[(Game, chess.Color)] =
    pairingAndNumber match
      case (pairing, number) =>
        for {
          user <- userRepo byId pairing.player.user orFail s"No user with id ${pairing.player.user}"
          hostColor = simul.hostColor | chess.Color.fromWhite(number % 2 == 0)
          whiteUser = hostColor.fold(host, user)
          blackUser = hostColor.fold(user, host)
          clock     = simul.clock.chessClockOf(hostColor)
          perfPicker =
            lila.game.PerfPicker.mainOrDefault(chess.Speed(clock.config), pairing.player.variant, none)
          game1 = Game.make(
            chess = chess
              .Game(
                variantOption = Some {
                  if (simul.position.isEmpty) pairing.player.variant
                  else chess.variant.FromPosition
                },
                fen = simul.position
              )
              .copy(clock = clock.start.some),
            whitePlayer = lila.game.Player.make(chess.White, whiteUser.some, perfPicker),
            blackPlayer = lila.game.Player.make(chess.Black, blackUser.some, perfPicker),
            mode = chess.Mode.Casual,
            source = lila.game.Source.Simul,
            pgnImport = None
          )
          game2 =
            game1
              .withId(pairing.gameId)
              .withSimulId(simul.id)
              .start
          _ <-
            (gameRepo insertDenormalized game2) >>-
              onGameStart(game2.id) >>-
              socket.startGame(simul, game2)
        } yield game2 -> hostColor

  private def update(simul: Simul): Funit =
    repo.update(simul) >>- socket.reload(simul.id) >>- publish()

  private def WithSimul(
      finding: SimulId => Fu[Option[Simul]],
      simulId: SimulId
  )(updating: Simul => Simul): Funit =
    workQueue(simulId) {
      finding(simulId) flatMapz { simul =>
        update(updating(simul))
      }
    }

  private object publish:
    private val siteMessage = SendToFlag("simul", Json.obj("t" -> "reload"))
    private val debouncer   = Debouncer[Unit](5 seconds, 1)(_ => Bus.publish(siteMessage, "sendToFlag"))
    def apply()             = debouncer.push(()).unit

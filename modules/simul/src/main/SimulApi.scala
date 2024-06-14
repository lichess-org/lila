package lila.simul

import play.api.libs.json.Json
import scala.concurrent.duration._

import shogi.variant.Variant
import lila.common.Bus
import lila.game.{ Game, GameRepo, PerfPicker }
import lila.hub.actorApi.timeline.{ Propagate, SimulCreate, SimulJoin }
import lila.memo.CacheApi._
import lila.user.{ User, UserRepo }

final class SimulApi(
    userRepo: UserRepo,
    gameRepo: GameRepo,
    onGameStart: lila.round.OnStart,
    socket: SimulSocket,
    timeline: lila.hub.actors.Timeline,
    repo: SimulRepo,
    cacheApi: lila.memo.CacheApi
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem,
    mode: play.api.Mode
) {

  private val workQueue =
    new lila.hub.DuctSequencers(
      maxSize = 128,
      expiration = 10 minutes,
      timeout = 10 seconds,
      name = "simulApi"
    )

  def currentHostIds: Fu[Set[String]] = currentHostIdsCache.get {}

  def find  = repo.find _
  def byIds = repo.byIds _

  private val currentHostIdsCache = cacheApi.unit[Set[User.ID]] {
    _.refreshAfterWrite(5 minutes)
      .buildAsyncFuture { _ =>
        repo.allStarted dmap (_.view.map(_.hostId).toSet)
      }
  }

  def create(setup: SimulForm.Setup, me: User): Fu[Simul] = {
    val simul = Simul.make(
      name = setup.name,
      clock = setup.clock,
      variants = setup.actualVariants,
      position = setup.position,
      host = me,
      color = setup.color,
      text = setup.text,
      estimatedStartAt = setup.estimatedStartAt,
      team = setup.team
    )
    repo.create(simul, me.hasGames) >>- {
      timeline ! (Propagate(SimulCreate(me.id, simul.id, simul.fullName)) toFollowersOf me.id)
    } inject simul
  }

  def update(prev: Simul, setup: SimulForm.Setup): Fu[Simul] = {
    val simul = prev.copy(
      name = setup.name,
      clock = setup.clock,
      variants = setup.actualVariants,
      position = setup.position,
      color = setup.color.some,
      text = setup.text,
      estimatedStartAt = setup.estimatedStartAt,
      team = setup.team
    )
    repo.update(simul) inject simul
  }

  def addApplicant(simulId: Simul.ID, user: User, variantKey: String): Funit =
    WithSimul(repo.findCreated, simulId) { simul =>
      if (simul.nbAccepted >= Game.maxPlayingRealtime) simul
      else {
        timeline ! (Propagate(SimulJoin(user.id, simul.id, simul.fullName)) toFollowersOf user.id)
        Variant(variantKey).filter(simul.variants.contains).fold(simul) { variant =>
          simul addApplicant SimulApplicant.make(
            SimulPlayer.make(
              user,
              variant,
              PerfPicker.mainOrDefault(
                speed = shogi.Speed(simul.clock.config.some),
                variant = variant,
                daysPerTurn = none
              )(user.perfs)
            )
          )
        }
      }
    }

  def removeApplicant(simulId: Simul.ID, user: User): Funit =
    WithSimul(repo.findCreated, simulId) { _ removeApplicant user.id }

  def accept(simulId: Simul.ID, userId: String, v: Boolean): Funit =
    userRepo byId userId flatMap {
      _ ?? { user =>
        WithSimul(repo.findCreated, simulId) { _.accept(user.id, v) }
      }
    }

  def start(simulId: Simul.ID): Funit =
    workQueue(simulId) {
      repo.findCreated(simulId) flatMap {
        _ ?? { simul =>
          simul.start ?? { started =>
            userRepo byId started.hostId orFail s"No such host: ${simul.hostId}" flatMap { host =>
              started.pairings.zipWithIndex.map(makeGame(started, host)).sequenceFu map { games =>
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
    }

  def onPlayerConnection(game: Game, user: Option[User])(simul: Simul): Unit =
    if (user.exists(simul.isHost) && simul.isRunning) {
      repo.setHostGameId(simul, game.id)
      socket.hostIsOn(simul.id, game.id)
    }

  def abort(simulId: Simul.ID): Funit =
    workQueue(simulId) {
      repo.findCreated(simulId) flatMap {
        _ ?? { simul =>
          (repo remove simul) >>- socket.aborted(simul.id)
        }
      }
    }

  def setText(simulId: Simul.ID, text: String): Funit =
    workQueue(simulId) {
      repo.find(simulId) flatMap {
        _ ?? { simul =>
          repo.setText(simul, text) >>- socket.reload(simulId)
        }
      }
    }

  def finishGame(game: Game): Funit =
    game.simulId ?? { simulId =>
      workQueue(simulId) {
        repo.findStarted(simulId) flatMap {
          _ ?? { simul =>
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
    }

  private def onComplete(simul: Simul): Unit = {
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
  }

  def ejectCheater(userId: String): Unit =
    repo.allNotFinished foreach {
      _ foreach { oldSimul =>
        workQueue(oldSimul.id) {
          repo.findCreated(oldSimul.id) flatMap {
            _ ?? { simul =>
              (simul ejectCheater userId) ?? { simul2 =>
                update(simul2).void
              }
            }
          }
        }
      }
    }

  def idToName(id: Simul.ID): Fu[Option[String]] =
    repo find id dmap2 { _.fullName }

  private def makeGame(simul: Simul, host: User)(
      pairingAndNumber: (SimulPairing, Int)
  ): Fu[(Game, shogi.Color)] =
    pairingAndNumber match {
      case (pairing, number) =>
        for {
          user <- userRepo byId pairing.player.user orFail s"No user with id ${pairing.player.user}"
          hostColor = simul.hostColor | shogi.Color.fromSente(number % 2 == 0)
          senteUser = hostColor.fold(host, user)
          goteUser  = hostColor.fold(user, host)
          clock     = simul.clock.shogiClockOf(hostColor)
          perfPicker =
            lila.game.PerfPicker.mainOrDefault(shogi.Speed(clock.config), pairing.player.variant, none)
          game1 = Game.make(
            shogi = shogi
              .Game(
                simul.position,
                pairing.player.variant
              )
              .copy(clock = clock.start.some),
            initialSfen = simul.position,
            sentePlayer = lila.game.Player.make(shogi.Sente, senteUser.some, perfPicker),
            gotePlayer = lila.game.Player.make(shogi.Gote, goteUser.some, perfPicker),
            mode = shogi.Mode.Casual,
            source = lila.game.Source.Simul,
            notationImport = None
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
    }

  private def update(simul: Simul) =
    repo.update(simul) >>- socket.reload(simul.id)

  private def WithSimul(
      finding: Simul.ID => Fu[Option[Simul]],
      simulId: Simul.ID
  )(updating: Simul => Simul): Funit = {
    workQueue(simulId) {
      finding(simulId) flatMap {
        _ ?? { simul =>
          update(updating(simul))
        }
      }
    }
  }
}

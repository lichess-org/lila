package lila.simul

import akka.actor._
import chess.variant.Variant
import play.api.libs.json.Json
import scala.concurrent.duration._

import lila.common.{ Bus, Debouncer }
import lila.db.dsl._
import lila.game.{ Game, GameRepo, PerfPicker }
import lila.hub.actorApi.timeline.{ Propagate, SimulCreate, SimulJoin }
import lila.hub.LightTeam.TeamID
import lila.memo.CacheApi._
import lila.socket.Socket.SendToFlag
import lila.user.{ User, UserRepo }

final class SimulApi(
    userRepo: UserRepo,
    gameRepo: GameRepo,
    onGameStart: lila.round.OnStart,
    socket: SimulSocket,
    renderer: lila.hub.actors.Renderer,
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
      position = setup.realPosition,
      host = me,
      color = setup.color,
      text = setup.text,
      team = setup.team,
      featurable = some(~setup.featured && canBeFeatured(me))
    )
    repo.create(simul) >>- publish() >>- {
      timeline ! (Propagate(SimulCreate(me.id, simul.id, simul.fullName)) toFollowersOf me.id)
    } inject simul
  }

  def update(prev: Simul, setup: SimulForm.Setup, me: User): Fu[Simul] = {
    val simul = prev.copy(
      name = setup.name,
      clock = setup.clock,
      variants = setup.actualVariants,
      position = setup.realPosition,
      color = setup.color.some,
      text = setup.text,
      team = setup.team,
      featurable = some(~setup.featured && canBeFeatured(me))
    )
    repo.update(simul) >>- publish() inject simul
  }

  private def canBeFeatured(user: User) = user.hasTitle && !user.lameOrTroll

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
                speed = chess.Speed(simul.clock.config.some),
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
          (repo remove simul) >>- socket.aborted(simul.id) >>- publish()
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

  def byTeamLeaders = repo.byTeamLeaders _

  def idToName(id: Simul.ID): Fu[Option[String]] =
    repo find id dmap2 { _.fullName }

  def teamOf(id: Simul.ID): Fu[Option[TeamID]] =
    repo.coll.primitiveOne[TeamID]($id(id), "team")

  private def makeGame(simul: Simul, host: User)(
      pairingAndNumber: (SimulPairing, Int)
  ): Fu[(Game, chess.Color)] =
    pairingAndNumber match {
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
    }

  private def update(simul: Simul): Funit =
    repo.update(simul) >>- socket.reload(simul.id) >>- publish()

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

  private object publish {
    private val siteMessage = SendToFlag("simul", Json.obj("t" -> "reload"))
    private val debouncer = system.actorOf(
      Props(
        new Debouncer(
          5 seconds,
          (_: Debouncer.Nothing) => Bus.publish(siteMessage, "sendToFlag")
        )
      )
    )
    def apply(): Unit = { debouncer ! Debouncer.Nothing }
  }
}

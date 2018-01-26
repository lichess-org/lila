package lila.simul

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.json.Json
import scala.concurrent.duration._

import chess.variant.Variant
import lila.common.Debouncer
import lila.game.{ Game, GameRepo, PerfPicker }
import lila.hub.actorApi.lobby.ReloadSimuls
import lila.hub.actorApi.map.Tell
import lila.hub.actorApi.timeline.{ Propagate, SimulCreate, SimulJoin }
import lila.socket.actorApi.SendToFlag
import lila.user.{ User, UserRepo }
import makeTimeout.short

final class SimulApi(
    system: ActorSystem,
    sequencers: ActorRef,
    onGameStart: Game.ID => Unit,
    socketHub: ActorRef,
    site: ActorSelection,
    renderer: ActorSelection,
    timeline: ActorSelection,
    userRegister: ActorSelection,
    lobby: ActorSelection,
    repo: SimulRepo,
    asyncCache: lila.memo.AsyncCache.Builder
) {

  def currentHostIds: Fu[Set[String]] = currentHostIdsCache.get

  def byIds = repo.byIds _

  private val currentHostIdsCache = asyncCache.single[Set[String]](
    name = "simul.currentHostIds",
    f = repo.allStarted dmap (_.map(_.hostId)(scala.collection.breakOut)),
    expireAfter = _.ExpireAfterAccess(10 minutes)
  )

  def create(setup: SimulSetup, me: User): Fu[Simul] = {
    val simul = Simul.make(
      clock = SimulClock(
        config = chess.Clock.Config(setup.clockTime * 60, setup.clockIncrement),
        hostExtraTime = setup.clockExtra * 60
      ),
      variants = setup.variants.flatMap { chess.variant.Variant(_) },
      host = me,
      color = setup.color
    )
    repo.createdByHostId(me.id) foreach {
      _.filter(_.isNotBrandNew).map(_.id).foreach(abort)
    }
    (repo create simul) >>- publish() >>- {
      timeline ! (Propagate(SimulCreate(me.id, simul.id, simul.fullName)) toFollowersOf me.id)
    } inject simul
  }

  def addApplicant(simulId: Simul.ID, user: User, variantKey: String): Unit = {
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
  }

  def removeApplicant(simulId: Simul.ID, user: User): Unit = {
    WithSimul(repo.findCreated, simulId) { _ removeApplicant user.id }
  }

  def accept(simulId: Simul.ID, userId: String, v: Boolean): Unit = {
    UserRepo byId userId foreach {
      _ foreach { user =>
        WithSimul(repo.findCreated, simulId) { _.accept(user.id, v) }
      }
    }
  }

  def start(simulId: Simul.ID): Unit = {
    Sequence(simulId) {
      repo.findCreated(simulId) flatMap {
        _ ?? { simul =>
          simul.start ?? { started =>
            UserRepo byId started.hostId flatten s"No such host: ${simul.hostId}" flatMap { host =>
              started.pairings.map(makeGame(started, host)).sequenceFu map { games =>
                games.headOption foreach {
                  case (game, _) => sendTo(simul.id, actorApi.StartSimul(game, simul.hostId))
                }
                games.foldLeft(started) {
                  case (s, (g, hostColor)) => s.setPairingHostColor(g.id, hostColor)
                }
              }
            } flatMap { s =>
              system.lilaBus.publish(Simul.OnStart(s), 'startSimul)
              update(s) >>- currentHostIdsCache.refresh
            }
          }
        }
      }
    }
  }

  def onPlayerConnection(game: Game, user: Option[User])(simul: Simul): Unit = {
    user.filter(simul.isHost) ifTrue simul.isRunning foreach { host =>
      repo.setHostGameId(simul, game.id)
      sendTo(simul.id, actorApi.HostIsOn(game.id))
    }
  }

  def abort(simulId: Simul.ID): Unit = {
    Sequence(simulId) {
      repo.findCreated(simulId) flatMap {
        _ ?? { simul =>
          (repo remove simul) >>- sendTo(simul.id, actorApi.Aborted) >>- publish()
        }
      }
    }
  }

  def finishGame(game: Game): Unit = {
    game.simulId foreach { simulId =>
      Sequence(simulId) {
        repo.findStarted(simulId) flatMap {
          _ ?? { simul =>
            val simul2 = simul.updatePairing(
              game.id,
              _.finish(game.status, game.winnerUserId, game.turns)
            )
            update(simul2) >>- {
              if (simul2.isFinished) onComplete(simul2)
            }
          }
        }
      }
    }
  }

  private def onComplete(simul: Simul): Unit = {
    currentHostIdsCache.refresh
    userRegister ! lila.hub.actorApi.SendTo(
      simul.hostId,
      lila.socket.Socket.makeMessage("simulEnd", Json.obj(
        "id" -> simul.id,
        "name" -> simul.name
      ))
    )
  }

  def ejectCheater(userId: String): Unit = {
    repo.allNotFinished foreach {
      _ foreach { oldSimul =>
        Sequence(oldSimul.id) {
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
  }

  def idToName(id: Simul.ID): Fu[Option[String]] =
    repo find id map2 { (simul: Simul) => simul.fullName }

  private def makeGame(simul: Simul, host: User)(pairing: SimulPairing): Fu[(Game, chess.Color)] = for {
    user ← UserRepo byId pairing.player.user flatten s"No user with id ${pairing.player.user}"
    hostColor = simul.hostColor
    whiteUser = hostColor.fold(host, user)
    blackUser = hostColor.fold(user, host)
    clock = simul.clock.chessClockOf(hostColor)
    perfPicker = lila.game.PerfPicker.mainOrDefault(chess.Speed(clock.config), pairing.player.variant, none)
    game1 = Game.make(
      chess = chess.Game(
        situation = chess.Situation(pairing.player.variant),
        clock = clock.start.some
      ),
      whitePlayer = lila.game.Player.make(chess.White, whiteUser.some, perfPicker),
      blackPlayer = lila.game.Player.make(chess.Black, blackUser.some, perfPicker),
      mode = chess.Mode.Casual,
      source = lila.game.Source.Simul,
      pgnImport = None
    )
    game2 = game1
      .withSimulId(simul.id)
      .withId(pairing.gameId)
      .start
    _ ← (GameRepo insertDenormalized game2) >>-
      onGameStart(game2.id) >>-
      sendTo(simul.id, actorApi.StartGame(game2, simul.hostId))
  } yield game2 -> hostColor

  private def update(simul: Simul) =
    repo.update(simul) >>- socketReload(simul.id) >>- publish()

  private def WithSimul(
    finding: Simul.ID => Fu[Option[Simul]],
    simulId: Simul.ID
  )(updating: Simul => Simul): Unit = {
    Sequence(simulId) {
      finding(simulId) flatMap {
        _ ?? { simul => update(updating(simul)) }
      }
    }
  }

  private def Sequence(simulId: Simul.ID)(work: => Funit): Unit = {
    sequencers ! Tell(simulId, lila.hub.Sequencer work work)
  }

  private object publish {
    private val siteMessage = SendToFlag("simul", Json.obj("t" -> "reload"))
    private val debouncer = system.actorOf(Props(new Debouncer(5 seconds, {
      (_: Debouncer.Nothing) =>
        site ! siteMessage
        repo.allCreated foreach { simuls =>
          renderer ? actorApi.SimulTable(simuls) map {
            case view: play.twirl.api.Html => ReloadSimuls(view.body)
          } pipeToSelection lobby
        }
    })))
    def apply(): Unit = { debouncer ! Debouncer.Nothing }
  }

  private def sendTo(simulId: Simul.ID, msg: Any): Unit = {
    socketHub ! Tell(simulId, msg)
  }

  private def socketReload(simulId: Simul.ID): Unit = {
    sendTo(simulId, actorApi.Reload)
  }
}

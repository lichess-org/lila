package lila.simul

import akka.actor._
import org.joda.time.DateTime
import play.api.libs.json._
import reactivemongo.bson._
import reactivemongo.core.commands._
import scala.concurrent.duration._

import chess.Status
import chess.variant.Variant
import lila.common.Debouncer
import lila.db.Types.Coll
import lila.game.{ Game, GameRepo }
import lila.hub.actorApi.map.Tell
import lila.socket.actorApi.SendToFlag
import lila.user.{ User, UserRepo }

private[simul] final class SimulApi(
    system: ActorSystem,
    sequencers: ActorRef,
    onGameStart: String => Unit,
    socketHub: ActorRef,
    site: ActorSelection,
    repo: SimulRepo) {

  def create(setup: SimulSetup, me: User): Fu[Simul] = {
    val simul = Simul.make(
      name = setup.name,
      clock = SimulClock(
        limit = setup.clockTime * 60,
        increment = setup.clockIncrement,
        hostExtraTime = setup.clockExtra * 60),
      variants = setup.variants.flatMap { chess.variant.Variant(_) },
      host = me)
    (repo create simul) >>- publish() inject simul
  }

  def addApplicant(simulId: Simul.ID, user: User, variantKey: String) {
    WithSimul(repo.findCreated, simulId) { simul =>
      Variant(variantKey).filter(simul.variants.contains).fold(simul) { variant =>
        simul addApplicant SimulApplicant(SimulPlayer(user, variant))
      }
    }
  }

  def removeApplicant(simulId: Simul.ID, user: User) {
    WithSimul(repo.findCreated, simulId) { _ removeApplicant user.id }
  }

  def accept(simulId: Simul.ID, userId: String, v: Boolean) {
    UserRepo byId userId foreach {
      _ foreach { user =>
        WithSimul(repo.findCreated, simulId) { _.accept(user.id, v) }
      }
    }
  }

  def start(simulId: Simul.ID) {
    Sequence(simulId) {
      repo.findCreated(simulId) flatMap {
        _ ?? { simul =>
          simul.start ?? { started =>
            update(started) >> {
              UserRepo byId started.hostId flatten s"No such host: ${simul.hostId}" flatMap { host =>
                started.pairings.map(makeGame(started, host)).sequenceFu.void
              }
            }
          }
        }
      }
    }
  }

  def finishGame(game: Game) {
    game.simulId foreach { simulId =>
      Sequence(simulId) {
        repo.findStarted(simulId) flatMap {
          _ ?? { simul =>
            val simul2 = simul.updatePairing(
              game.id,
              _.finish(game.status, game.winnerUserId, game.turns)
            )
            update(simul2).void
          }
        }
      }
    }
  }

  def ejectCheater(userId: String) {
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

  private def makeGame(simul: Simul, host: User)(pairing: SimulPairing) = for {
    user ← UserRepo byId pairing.player.user flatten s"No user with id ${pairing.player.user}"
    game1 = Game.make(
      game = chess.Game(
        board = chess.Board init pairing.player.variant,
        clock = simul.clock.chessClock.start.some),
      whitePlayer = lila.game.Player.white,
      blackPlayer = lila.game.Player.black,
      mode = chess.Mode.Casual,
      variant = pairing.player.variant,
      source = lila.game.Source.Simul,
      pgnImport = None)
    game2 = game1
      .updatePlayer(chess.White, _.withUser(host.id, lila.game.PerfPicker.mainOrDefault(game1)(host.perfs)))
      .updatePlayer(chess.Black, _.withUser(user.id, lila.game.PerfPicker.mainOrDefault(game1)(user.perfs)))
      .withSimulId(simul.id)
      .withId(pairing.gameId)
      .start
    _ ← (GameRepo insertDenormalized game2) >>-
      onGameStart(game2.id) >>-
      sendTo(simul.id, actorApi.StartGame(game2))
  } yield game2

  private def update(simul: Simul) =
    repo.update(simul) >>- socketReload(simul.id) >>- publish()

  private def WithSimul(
    finding: Simul.ID => Fu[Option[Simul]],
    simulId: Simul.ID)(updating: Simul => Simul) {
    Sequence(simulId) {
      finding(simulId) flatMap {
        _ ?? { simul => update(updating(simul)) }
      }
    }
  }

  private def Sequence(simulId: Simul.ID)(work: => Funit) {
    sequencers ! Tell(simulId, lila.hub.Sequencer work work)
  }

  private object publish {
    private val siteMessage = SendToFlag("simul", Json.obj("t" -> "reload"))
    private val debouncer = system.actorOf(Props(new Debouncer(2 seconds, {
      (_: Debouncer.Nothing) =>
        site ! siteMessage
    })))
    def apply() { debouncer ! Debouncer.Nothing }
  }

  private def sendTo(simulId: Simul.ID, msg: Any) {
    socketHub ! Tell(simulId, msg)
  }

  private def socketReload(simulId: Simul.ID) {
    sendTo(simulId, actorApi.Reload)
  }
}

package lila.simul

import akka.actor.ActorRef
import org.joda.time.DateTime
import reactivemongo.bson._
import reactivemongo.core.commands._
import scala.concurrent.duration._

import chess.Status
import chess.variant.Variant
import lila.db.Types.Coll
import lila.game.{ Game, GameRepo }
import lila.user.{ User, UserRepo }

private[simul] final class SimulApi(
    sequencers: ActorRef,
    onGameStart: String => Unit,
    simulColl: Coll) {

  import lila.db.BSON.BSONJodaDateTimeHandler
  import reactivemongo.bson.Macros
  private implicit val StatusBSONHandler = new BSONHandler[BSONInteger, Status] {
    def read(bsonInt: BSONInteger): Status = Status(bsonInt.value) err s"No such status: ${bsonInt.value}"
    def write(x: Status) = BSONInteger(x.id)
  }
  private implicit val VariantBSONHandler = new BSONHandler[BSONInteger, Variant] {
    def read(bsonInt: BSONInteger): Variant = Variant(bsonInt.value) err s"No such variant: ${bsonInt.value}"
    def write(x: Variant) = BSONInteger(x.id)
  }
  private implicit val ClockBSONHandler = Macros.handler[SimulClock]
  private implicit val PlayerBSONHandler = Macros.handler[SimulPlayer]
  private implicit val ApplicantBSONHandler = Macros.handler[SimulApplicant]
  private implicit val PairingBSONHandler = Macros.handler[SimulPairing]
  private implicit val SimulBSONHandler = Macros.handler[Simul]

  def find(id: Simul.ID): Fu[Option[Simul]] =
    simulColl.find(BSONDocument("_id" -> id)).one[Simul]

  def findStarted(id: Simul.ID): Fu[Option[Simul]] =
    find(id) map (_ filter (_.isStarted))

  def findOpen(id: Simul.ID): Fu[Option[Simul]] =
    find(id) map (_ filter (_.isOpen))

  def allNotFinished =
    simulColl.find(
      BSONDocument("finishedAt" -> BSONDocument("$exists" -> false))
    ).cursor[Simul].collect[List]()

  def createSimul(setup: SimulSetup, me: User): Fu[Simul] = {
    val simul = Simul.make(
      name = setup.name,
      clock = SimulClock(setup.clockTime * 60, setup.clockIncrement),
      variants = setup.variants.flatMap(chess.variant.Variant.apply),
      host = me)
    simulColl insert simul inject simul
  }

  def update(simul: Simul) =
    simulColl.update(BSONDocument("_id" -> simul.id), simul).void

  def addApplicant(simulId: Simul.ID, user: User, variant: Variant) {
    WithSimul(findOpen, simulId) { _ addApplicant SimulApplicant(SimulPlayer(user, variant)) }
  }

  def removeApplicant(simulId: Simul.ID, user: User) {
    WithSimul(findOpen, simulId) { _ removeApplicant user.id }
  }

  def accept(simulId: Simul.ID, user: User, v: Boolean) {
    WithSimul(findOpen, simulId) { _.accept(user.id, v) }
  }

  def start(simulId: Simul.ID) {
    Sequence(simulId) {
      findOpen(simulId) flatMap {
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
        findStarted(simulId) flatMap {
          _ ?? { simul =>
            val simul2 = simul.updatePairing(
              game.id,
              _.finish(game.status, game.winnerUserId, game.turns))
            update(simul2).void >>- socketReload(simul2.id)
          }
        }
      }
    }
  }

  def ejectCheater(userId: String) {
    allNotFinished foreach {
      _ foreach { oldSimul =>
        Sequence(oldSimul.id) {
          findOpen(oldSimul.id) flatMap {
            _ ?? { simul =>
              (simul ejectCheater userId) ?? { simul2 =>
                update(simul2).void >>- socketReload(simul2.id)
              }
            }
          }
        }
      }
    }
  }

  private def makeGame(simul: Simul, host: User)(pairing: SimulPairing) = for {
    user ← getUser(pairing.player.user)
    game1 = Game.make(
      game = chess.Game(
        board = chess.Board init pairing.player.variant,
        clock = simul.clock.chessClock.some),
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
    _ ← (GameRepo insertDenormalized game2) >>- onGameStart(game2.id)
  } yield game2

  private def getUser(userId: String): Fu[User] =
    UserRepo byId userId flatten s"No user with id $userId"

  private def WithSimul(
    finding: Simul.ID => Fu[Option[Simul]],
    simulId: Simul.ID)(updating: Simul => Simul) {
    Sequence(simulId) {
      finding(simulId) flatMap {
        _ ?? { simul => update(updating(simul)) }
      }
    }
  }

  private def Sequence(simulId: String)(work: => Funit) {
    sequencers ! lila.hub.actorApi.map.Tell(simulId, lila.hub.Sequencer work work)
  }

  private def sendTo(simulId: String, msg: Any) {
    // socketHub ! Tell(simulId, msg)
  }

  private def socketReload(simulId: String) {
    sendTo(simulId, actorApi.Reload)
  }
}

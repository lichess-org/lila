package lila.relay

import akka.actor._
import akka.pattern.{ ask, pipe }
import scala.concurrent.duration._

import lila.hub.SequentialActor

private[relay] final class GameActor(
    fics: ActorRef,
    ficsId: Int,
    getGameId: () => Fu[Option[String]],
    importer: Importer) extends SequentialActor {

  import GameActor._

  context setReceiveTimeout 3.hours

  override def preStart() {
    context.system.lilaBus.subscribe(self, 'relayMove)
    fics ! FICS.Observe(ficsId)
  }

  override def postStop() {
    context.system.lilaBus unsubscribe self
  }

  def process = {

    case move@FICS.Move(id, san, ply, _) if id == ficsId => withGameId { gameId =>
      importer.move(gameId, san, ply) >>- println(s"http://en.l.org/$gameId $ply: $san")
    }

    case data: command.Moves.Game => recover(data)

    case Recover =>
      implicit val t = makeTimeout seconds 60
      fics ? command.Moves(ficsId) mapTo manifest[command.Moves.Game] flatMap recover

    case ReceiveTimeout => fuccess {
      self ! SequentialActor.Terminate
    }
  }

  override def onFailure(e: Exception) {
    println(s"[$ficsId] ERR ${e.getMessage}")
  }

  def recover(data: command.Moves.Game) = withGameId { gameId =>
      importer.full(gameId, data)
    }

  def withGameId[A](f: String => Fu[A]): Funit = getGameId() flatMap {
    case None         => fufail(s"No game found for FICS ID $ficsId")
    case Some(gameId) => f(gameId).void
  }
}

object GameActor {

  case object Recover
}

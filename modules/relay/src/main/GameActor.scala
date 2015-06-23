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
    import makeTimeout.larger
    context.system.lilaBus.subscribe(self, 'relayMove)
    fics ! FICS.Observe(ficsId)
    fics ? command.Moves(ficsId) pipeTo self
  }

  override def postStop() {
    context.system.lilaBus unsubscribe self
  }

  def process = {

    case FICS.Move(id, san, ply) if id == ficsId => withGameId { gameId =>
      importer.move(gameId, san, ply) >>- println(s"http://en.l.org/$gameId $ply: $san")
    }

    case data: command.Moves.Game => withGameId { gameId =>
      importer.full(gameId, data) >>- println(s"http://en.l.org/$gameId")
    }

    case Up => funit // just making sure the actor is up }

    case ReceiveTimeout => fuccess {
      self ! SequentialActor.Terminate
    }
  }

  def withGameId[A](f: String => Fu[A]): Funit = getGameId() flatMap {
    case None         => fufail(s"No game found for FICS ID $ficsId")
    case Some(gameId) => f(gameId).void
  }
}

object GameActor {

  case object Up
}

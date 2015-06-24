package lila.relay

import akka.actor._
import akka.pattern.{ ask, pipe }
import scala.concurrent.duration._

import lila.hub.SequentialActor

private[relay] final class GameActor(
    fics: ActorRef,
    ficsId: Int,
    getRelayGame: () => Fu[Option[Relay.Game]],
    importer: Importer) extends SequentialActor {

  import GameActor._

  context setReceiveTimeout 3.hours

  override def preStart() {
    fics ! FICS.Observe(ficsId)
  }

  def process = {

    case move@GameEvent.Move(_, san, ply, _) => withRelayGame { g =>
      importer.move(g.id, san, ply) >>- println(s"http://en.l.org/${g.id} $ply: $san")
    }

    case move@GameEvent.Draw(_) => withRelayGame { g =>
      fuccess {
        println(s"http://en.l.org/${g.id} draw")
        importer.draw(g.id) >>- {
          self ! SequentialActor.Terminate
        }
      }
    }

    case move@GameEvent.Resign(_, loser) => withRelayGame { g =>
      println(s"http://en.l.org/${g.id} $loser resigns")
      g colorOf loser match {
        case None => fufail(s"Invalid loser $loser")
        case Some(color) => importer.resign(g.id, color) >>- {
          self ! SequentialActor.Terminate
        }
      }
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

  def recover(data: command.Moves.Game) = withRelayGame { g =>
    importer.full(g.id, data)
  }

  def withRelayGame[A](f: Relay.Game => Fu[A]): Funit = getRelayGame() flatMap {
    case None    => fufail(s"No game found for FICS ID $ficsId")
    case Some(g) => f(g).void
  }
}

object GameActor {

  case object Recover
}

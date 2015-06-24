package lila.relay

import akka.actor._
import akka.pattern.{ ask, pipe }
import scala.concurrent.duration._

import lila.hub.SequentialActor

private[relay] final class GameActor(
    fics: ActorRef,
    ficsId: Int,
    relayId: String,
    getRelayGame: () => Fu[Option[Relay.Game]],
    setEnd: () => Funit,
    importer: Importer) extends SequentialActor {

  import GameActor._

  context setReceiveTimeout 3.hours

  override def preStart() {
    fics ! FICS.Observe(ficsId).pp
  }

  def process = {

    case move@GameEvent.Move(_, san, ply, _, white, black) => withRelayGame { g =>
      if (g.white == white && g.black == black)
        importer.move(g.id, san, ply) >>- println(s"http://en.l.org/${g.id} $ply: $san")
      else {
        println(g, white, black)
        end
      }
    }

    case move@GameEvent.Draw(_) => withRelayGame { g =>
      fuccess {
        println(s"http://en.l.org/${g.id} draw")
        importer.draw(g.id) >> end
      }
    }

    case move@GameEvent.Resign(_, loser) => withRelayGame { g =>
      println(s"http://en.l.org/${g.id} $loser resigns")
      g colorOf loser match {
        case None        => end
        case Some(color) => importer.resign(g.id, color) >> end
      }
    }

    case Recover =>
      implicit val t = makeTimeout seconds 60
      fics ? command.Moves(ficsId) mapTo
        manifest[command.Moves.Game] flatMap { data =>
          withRelayGame { g =>
            if (g.white == data.white.ficsName && g.black == data.black.ficsName)
              importer.full(relayId, g.id, data)
            else {
              println(g, data)
              end
            }
          }
        }

    case ReceiveTimeout => end
  }

  override def onFailure(e: Exception) {
    println(s"[$ficsId] ERR ${e.getMessage}")
  }

  def end = setEnd() >>- {
    println(s"[$ficsId] End actor $self")
    self ! SequentialActor.Terminate
  }

  def withRelayGame[A](f: Relay.Game => Fu[A]): Funit = getRelayGame() flatMap {
    case None    => fufail(s"No game found for FICS ID $ficsId")
    case Some(g) => f(g).void
  }
}

object GameActor {

  case object Recover
}

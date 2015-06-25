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
    // println(s"[$ficsId] start actor $self")
    fics ! FICS.Observe(ficsId)
  }

  def process = {

    case move: GameEvent.Move => withRelayGame { g =>
      if (g.white == move.white && g.black == move.black)
        importer.move(g.id, move.san, move.ply)
      else end
    }

    case move@GameEvent.Draw(_) => withRelayGame { g =>
      fuccess {
        println(s"[$ficsId] http://en.l.org/${g.id} draw")
        importer.draw(g.id) >> end
      }
    }

    case move@GameEvent.Resign(_, loser) => withRelayGame { g =>
      println(s"[$ficsId] http://en.l.org/${g.id} $loser resigns")
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
              importer.full(relayId, g.id, data) addEffect {
                case true =>
                  // re-observe. If a limit was reached before,
                  // but a slot became available, use it.
                  fics ! FICS.Observe(ficsId)
                case false =>
              }
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
    // println(s"[$ficsId] stop actor $self")
    fics ! FICS.Unobserve(ficsId)
    self ! SequentialActor.Terminate
  }

  def withRelayGame[A](f: Relay.Game => Fu[A]): Funit = getRelayGame() flatMap {
    case None    => fufail(s"[$ficsId] No game found!")
    case Some(g) => f(g).void
  }
}

object GameActor {

  case object Recover
}

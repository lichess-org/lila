package lila.relay

import akka.actor._
import akka.pattern.{ ask, pipe }
import scala.concurrent.duration._

import lila.hub.actorApi.map.Tell
import lila.hub.SequentialActor

private[relay] final class TourneyActor(
    id: String,
    ficsProps: Props,
    repo: RelayRepo,
    importer: Importer) extends SequentialActor {

  import TourneyActor._

  context setReceiveTimeout 1.hour

  val fics = context.actorOf(ficsProps, name = "fics")

  val gameMap = context.actorOf(Props(new lila.hub.ActorMap {
    def mkActor(ficsIdStr: String) = {
      val ficsId = parseIntOption(ficsIdStr) err s"Invalid relay FICS id $ficsIdStr"
      new GameActor(
        fics = fics,
        ficsId = ficsId,
        relayId = id,
        getRelayGame = () => repo.gameByFicsId(id, ficsId),
        setEnd = () => repo.endGameByFicsId(id, ficsId),
        importer = importer)
    }
    def receive = actorMapReceive
  }), name = "games")

  def process = {

    case event: GameEvent => fuccess {
      gameMap ! Tell(event.ficsId.toString, event)
    }

    case Recover => withRelay { relay =>
      implicit val t = makeTimeout seconds 30
      fics ? command.ListGames(relay.ficsId) mapTo
        manifest[command.ListGames.Result] flatMap { games =>
          val rgs = games.map { g =>
            relay gameByFicsId g.ficsId match {
              case None     => Relay.Game.make(g.ficsId, g.white, g.black)
              case Some(rg) => rg
            }
          }
          val nr = relay.copy(games = rgs)
          // println(s"[relay] ${nr.name}: ${nr.activeGames.size}/${nr.games.size} games")
          repo.setGames(nr) >>-
            nr.activeGames.foreach { rg =>
              gameMap ! Tell(rg.ficsId.toString, GameActor.Recover)
            }
        }
    }

    case ReceiveTimeout => fuccess {
      self ! SequentialActor.Terminate
    }
  }

  def withRelay[A](f: Relay => Fu[A]): Funit = repo byId id flatMap {
    case None        => fufail(s"No relay found for ID $id")
    case Some(relay) => f(relay).void
  }

  override def onFailure(e: Exception) {
    println(s"[$id] ERR ${e.getMessage}")
  }
}

object TourneyActor {

  case object Recover
}

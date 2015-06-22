package lila.relay

import akka.actor._
import scala.concurrent.duration._

import lila.hub.SequentialActor

private[relay] final class GameActor(
    ficsId: Int,
    getGameId: Int => Fu[Option[String]],
    importer: Importer) extends SequentialActor {

  import GameActor._

  context setReceiveTimeout 3.hours

  def process = {

    case Move(str) => getGameId(ficsId) flatMap {
      case None => fufail(s"No game found for FICS ID $ficsId")
      case Some(gameId) =>
        println(s"http://en.l.org/$gameId")
        importer.move(gameId, str)
    }

    case Import(gameId, data) => importer(gameId)(data).void

    case ReceiveTimeout => fuccess {
      self ! SequentialActor.Terminate
    }
  }
}

object GameActor {

  case class Import(
    gameId: String,
    data: command.Moves.Game)

  case class Move(str: String)
}

package lila.fishnet

import org.joda.time.DateTime

import chess.format.FEN
import chess.variant.Variant

sealed trait Work {
  val game: Work.Game
}

object Work {

  case class Acquired(
    clientKey: Client.Key,
    date: DateTime)

  case class Game(
    id: String,
    position: Option[FEN],
    variant: Variant,
    moves: Seq[String])

  case class Move(
      _id: String, // random
      game: Game,
      level: Int,
      tries: Int,
      acquired: Option[Acquired],
      createdAt: DateTime) extends Work {

    def id = _id

    def assignTo(client: Client) = copy(
      acquired = Acquired(clientKey = client.key, date = DateTime.now).some,
      tries = tries + 1)
  }

  case class Analysis(
      _id: String, // random
      game: Game,
      tries: Int,
      acquired: Option[Acquired],
      createdAt: DateTime) extends Work {

    def id = _id

    def assignTo(client: Client) = copy(
      acquired = Acquired(clientKey = client.key, date = DateTime.now).some,
      tries = tries + 1)
  }

  def makeId = scala.util.Random.alphanumeric take 8 mkString
}

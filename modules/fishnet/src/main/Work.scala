package lila.fishnet

import org.joda.time.DateTime

import chess.format.Uci
import chess.variant.Variant

sealed trait Work

object Work {

  case class Acquired(
    clientKey: Client.Key,
    date: DateTime)

  case class Move(
      _id: String, // random
      gameId: String,
      position: Option[String],
      variant: Variant,
      moves: List[Uci],
      tries: Int,
      acquired: Option[Acquired],
      createdAt: DateTime) extends Work {

    def id = _id

    def assignTo(client: Client) =
      copy(acquired = Acquired(clientKey = client.key, date = DateTime.now).some)
  }

  case class Analysis(
      _id: String, // random
      gameId: String,
      position: Option[String],
      variant: Variant,
      moves: List[Uci],
      tries: Int,
      acquired: Option[Acquired],
      createdAt: DateTime) extends Work {

    def id = _id

    def acquire(client: Client) =
      copy(acquired = Acquired(clientKey = client.key, date = DateTime.now).some)
  }
}

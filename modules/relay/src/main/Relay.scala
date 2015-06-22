package lila.relay

import org.joda.time.DateTime

import ornicar.scalalib.Random

case class Relay(
  id: String, // random ID
  ficsId: Int,
  name: String,
  status: Relay.Status,
  date: DateTime,
  games: List[RelayGame])

object Relay {

  def make(ficsId: Int, name: String, status: Status) = Relay(
    id = Random nextStringUppercase 8,
    ficsId = ficsId,
    name = name,
    status = status,
    date = DateTime.now,
    games = Nil)

  sealed abstract class Status(val id: Int)
  object Status {
    object Unknown extends Status(0)
    object Created extends Status(10)
    object Started extends Status(20)
    object Finished extends Status(30)
    val all = List(Unknown, Created, Started, Finished)
    def apply(id: Int) = all.find(_.id == id)
  }
}

package lila.relay

import org.joda.time.DateTime

import ornicar.scalalib.Random

case class RelayGame(
  id: String, // lichess game ID
  ficsId: Int,
  white: String,
  black: String,
  // status: chess.Status,
  date: DateTime)

// object Tourney {

//   def make(ficsId: Int, name: String, status: Status) = Tourney(
//     id = Random nextStringUppercase 8,
//     ficsId = ficsId,
//     name = name,
//     status = status,
//     createdAt = DateTime.now,
//     updatedAt = DateTime.now)

//   sealed abstract class Status(val id: Int)
//   object Status {
//     object Created extends Status(10)
//     object Started extends Status(20)
//     object Finished extends Status(30)
//     object Hidden extends Status(40)
//     val all = List(Created, Started, Finished, Hidden)
//     def apply(id: Int) = all.find(_.id == id)
//   }
// }

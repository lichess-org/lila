package lila.game

import java.security.MessageDigest
import lila.db.ByteArray
import org.joda.time.DateTime

private[game] case class Metadata(
    source: Option[Source],
    pgnImport: Option[PgnImport],
    relay: Option[Relay],
    tournamentId: Option[String],
    simulId: Option[String],
    tvAt: Option[DateTime],
    analysed: Boolean) {

  def pgnDate = pgnImport flatMap (_.date)

  def pgnUser = pgnImport flatMap (_.user)

  def isEmpty = this == Metadata.empty
}

private[game] object Metadata {

  val empty = Metadata(None, None, None, None, None, None, false)
}

case class Relay(id: String, white: Relay.Player, black: Relay.Player) {

  def withTenths(color: chess.Color, tenths: Int) = copy(
    white = (color == chess.White).fold(white withTenths tenths, white),
    black = (color == chess.Black).fold(black withTenths tenths, black))
}

object Relay {

  case class Player(
      name: String,
      title: Option[String],
      rating: Option[Int],
      tenths: Option[Int]) {

    def withTenths(t: Int) = copy(tenths = t.some)

    def seconds = tenths.map(_.toFloat / 10)
  }

  import reactivemongo.bson.Macros
  import ByteArray.ByteArrayBSONHandler
  implicit val relayPlayerBSONHandler = Macros.handler[Relay.Player]
  implicit val relayBSONHandler = Macros.handler[Relay]
}

case class PgnImport(
  user: Option[String],
  date: Option[String],
  pgn: String,
  // hashed PGN for DB unicity
  h: Option[ByteArray])

object PgnImport {

  def hash(pgn: String) = ByteArray {
    MessageDigest getInstance "MD5" digest
      pgn.lines.map(_.replace(" ", "")).filter(_.nonEmpty).mkString("\n").getBytes("UTF-8") take 12
  }

  def make(
    user: Option[String],
    date: Option[String],
    pgn: String) = PgnImport(
    user = user,
    date = date,
    pgn = pgn,
    h = hash(pgn).some)

  import reactivemongo.bson.Macros
  import ByteArray.ByteArrayBSONHandler
  implicit val pgnImportBSONHandler = Macros.handler[PgnImport]
}

package lila.game

import java.security.MessageDigest
import lila.db.ByteArray

private[game] case class Metadata(
    source: Option[Source],
    pgnImport: Option[PgnImport],
    tournamentId: Option[String],
    swissId: Option[String],
    simulId: Option[String],
    analysed: Boolean
) {

  def pgnDate = pgnImport flatMap (_.date)

  def pgnUser = pgnImport flatMap (_.user)

  def isEmpty = this == Metadata.empty
}

private[game] object Metadata {

  val empty = Metadata(None, None, None, None, None, analysed = false)
}

case class PgnImport(
    user: Option[String],
    date: Option[String],
    pgn: String,
    // hashed PGN for DB unicity
    h: Option[ByteArray]
)

object PgnImport {

  def hash(pgn: String) =
    ByteArray {
      MessageDigest getInstance "MD5" digest {
        pgn.linesIterator
          .map(_.replace(" ", ""))
          .filter(_.nonEmpty)
          .to(List)
          .mkString("\n")
          .getBytes("UTF-8")
      } take 12
    }

  def make(
      user: Option[String],
      date: Option[String],
      pgn: String
  ) =
    PgnImport(
      user = user,
      date = date,
      pgn = pgn,
      h = hash(pgn).some
    )

  import reactivemongo.api.bson.Macros
  import ByteArray.ByteArrayBSONHandler
  implicit val pgnImportBSONHandler = Macros.handler[PgnImport]
}

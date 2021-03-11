package lila.game

import java.security.MessageDigest
import lila.db.ByteArray
import chess.Color

private[game] case class Metadata(
    source: Option[Source],
    pgnImport: Option[PgnImport],
    tournamentId: Option[String],
    swissId: Option[String],
    simulId: Option[String],
    analysed: Boolean,
    drawOffers: GameDrawOffers
) {

  def pgnDate = pgnImport flatMap (_.date)

  def pgnUser = pgnImport flatMap (_.user)

  def isEmpty = this == Metadata.empty
}

private[game] object Metadata {

  val empty = Metadata(None, None, None, None, None, analysed = false, GameDrawOffers.empty)
}

// plies
case class GameDrawOffers(white: Set[Int], black: Set[Int]) {

  def lastBy(color: Color): Option[Int] = color.fold(white, black).maxOption

  def add(color: Color, ply: Int) =
    color.fold(copy(white = white incl ply), copy(black = black incl ply))

  def isEmpty = this == GameDrawOffers.empty

  // lichess allows to offer draw on either turn,
  // normalize to pretend it was done on the opponent turn.
  def normalize(color: Color): Set[Int] = color.fold(white, black) map {
    case ply if (ply % 2 == 0) == color.white => ply + 1
    case ply => ply
  }
  def normalizedPlies: Set[Int] = normalize(chess.White) ++ normalize(chess.Black)

  def nonEmptyNormalizedPlies = !isEmpty option normalizedPlies
}

object GameDrawOffers {
  val empty = GameDrawOffers(Set.empty, Set.empty)
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

package lila.game

import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import lila.db.ByteArray
import chess.{ Ply, Color }
import chess.format.pgn.PgnStr

private[game] case class Metadata(
    source: Option[Source],
    pgnImport: Option[PgnImport],
    tournamentId: Option[TourId],
    swissId: Option[SwissId],
    simulId: Option[SimulId],
    analysed: Boolean,
    drawOffers: GameDrawOffers,
    rules: Set[GameRule]
):

  def pgnDate = pgnImport.flatMap(_.date)

  def pgnUser = pgnImport.flatMap(_.user)

  def isEmpty = this == Metadata.empty

  def hasRule(rule: GameRule.type => GameRule) = rules(rule(GameRule))
  def nonEmptyRules                            = rules.nonEmpty option rules

private[game] object Metadata:

  val empty =
    Metadata(None, None, None, None, None, analysed = false, GameDrawOffers.empty, rules = Set.empty)

case class GameDrawOffers(white: Set[Ply], black: Set[Ply], lastDrawColor: Option[Color]):

  def lastBy(color: Color): Option[Ply] = color.fold(white, black).maxOption(intOrdering)

  def lastAddedDrawColorIs(color: Color) =
    lastDrawColor.contains(color)

  def add(color: Color, ply: Ply) =
    color.fold(copy(white = white incl ply, lastDrawColor = Some(color)), 
      copy(black = black incl ply, lastDrawColor = Some(color)))

  def isEmpty = this == GameDrawOffers.empty

  // lichess allows to offer draw on either turn,
  // normalize to pretend it was done on the opponent turn.
  def normalize(color: Color): Set[Ply] = color.fold(white, black) map {
    case ply if ply.turn == color => ply + 1
    case ply                      => ply
  }
  def normalizedPlies: Set[Ply] = normalize(chess.White) ++ normalize(chess.Black)

object GameDrawOffers:
  val empty = GameDrawOffers(Set.empty, Set.empty, None)

enum GameRule:
  case NoAbort, NoRematch, NoGiveTime, NoClaimWin, NoEarlyDraw
  val key = lila.common.String lcfirst toString
object GameRule:
  val byKey = values.mapBy(_.key)

case class PgnImport(
    user: Option[UserId],
    date: Option[String],
    pgn: PgnStr,
    // hashed PGN for DB unicity
    h: Option[ByteArray]
)

object PgnImport:

  def hash(pgn: PgnStr) = ByteArray {
    MessageDigest getInstance "MD5" digest {
      pgn.value.linesIterator
        .map(_.replace(" ", ""))
        .filter(_.nonEmpty)
        .to(List)
        .mkString("\n")
        .getBytes(UTF_8)
    } take 12
  }

  def make(user: Option[UserId], date: Option[String], pgn: PgnStr) =
    PgnImport(
      user = user,
      date = date,
      pgn = pgn,
      h = hash(pgn).some
    )

  import reactivemongo.api.bson.*
  import lila.db.dsl.given
  given BSONDocumentHandler[PgnImport] = Macros.handler

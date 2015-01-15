package lila.opening

import chess.Color
import org.joda.time.DateTime

import lila.rating.Perf

case class Move(
  first: String,
  cp: Int,
  line: List[String])

case class Opening(
    id: Opening.ID,
    fen: String,
    moves: List[Move],
    color: Color,
    date: DateTime,
    perf: Perf,
    attempts: Int,
    wins: Int) {

  lazy val goal = qualityMoves.count(_.quality == Quality.Good) min 4

  lazy val qualityMoves: List[QualityMove] = {
    val bestCp = moves.foldLeft(Int.MaxValue) {
      case (cp, move) => if (move.cp < cp) move.cp else cp
    }
    moves.map { move =>
      QualityMove(move, Quality(move.cp, bestCp))
    }
  }

  def winPercent = if (attempts == 0) 0 else wins * 100 / attempts
}

sealed abstract class Quality(val threshold: Int) {
  val name = toString.toLowerCase
}
object Quality {
  case object Sharp extends Quality(5)

  case object Good extends Quality(30)
  case object Dubious extends Quality(70)
  case object Bad extends Quality(Int.MaxValue)

  val Positive = 0
  val OpMinorAdv = 10 // Cp for opponent to have minor adv
  val OpMajorAdv = 30 // Cp for opponent to have considerable adv

  def apply(moveCp: Int, bestCp: Int) = {
    val dif = moveCp - bestCp

    if      (bestCp <= Positive   && moveCp <= OpMinorAdv && dif < Good.threshold) Good // You have the advantage, don't want to give minor
    else if (bestCp <= OpMinorAdv && moveCp <= OpMajorAdv && dif < Good.threshold) Good // Op has up to minor adv, cannot give major adv
    else if (dif < Sharp.threshold) Good // Op has major adv, important to not lose any adv
    else if (dif < Dubious.threshold) Dubious
    else Bad
  }
}

case class QualityMove(
  move: Move,
  quality: Quality)

object Opening {

  type ID = Int

  def make(
    fen: String,
    color: Color,
    moves: List[Move])(id: ID) = new Opening(
    id = id,
    fen = fen,
    moves = moves,
    color = color,
    date = DateTime.now,
    perf = Perf.default,
    attempts = 0,
    wins = 0)

  import reactivemongo.bson._
  import lila.db.BSON
  import BSON.BSONJodaDateTimeHandler

  implicit val moveBSONHandler = new BSON[Move] {

    def reads(r: BSON.Reader): Move = Move(
      first = r str "first",
      cp = r int "cp",
      line = chess.format.pgn.Binary.readMoves(r.bytes("line").value.toList).get)

    def writes(w: BSON.Writer, o: Move) = BSONDocument(
      "first" -> o.first,
      "cp" -> o.cp,
      "line" -> lila.db.ByteArray {
        chess.format.pgn.Binary.writeMoves(o.line).get.toArray
      })
  }

  object BSONFields {
    val id = "_id"
    val fen = "fen"
    val moves = "moves"
    val white = "white"
    val date = "date"
    val attempts = "attempts"
    val wins = "wins"
    val perf = "perf"
    val rating = s"$perf.gl.r"
  }

  implicit val openingBSONHandler = new BSON[Opening] {

    import BSONFields._
    import Perf.perfBSONHandler

    def reads(r: BSON.Reader): Opening = Opening(
      id = r int id,
      fen = r str fen,
      moves = r.get[List[Move]](moves),
      color = Color(r bool white),
      date = r date date,
      perf = r.get[Perf](perf),
      attempts = r int attempts,
      wins = r int wins)

    def writes(w: BSON.Writer, o: Opening) = BSONDocument(
      id -> o.id,
      fen -> o.fen,
      moves -> o.moves,
      white -> o.color.white,
      date -> o.date,
      perf -> o.perf,
      attempts -> o.attempts,
      wins -> o.wins)
  }
}

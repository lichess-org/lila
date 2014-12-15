package lila.opening

import chess.Color
import org.joda.time.DateTime

case class Move(
  first: String,
  cp: Int,
  line: List[String])

case class Opening(
    id: Opening.ID,
    fen: String,
    ply: Int,
    moves: List[Move],
    color: Color,
    date: DateTime,
    attempts: Int,
    score: Double) {

}

object Opening {

  type ID = Int

  def make(
    fen: String,
    ply: Int,
    color: Color,
    moves: List[Move])(id: ID) = new Opening(
    id = id,
    fen = fen,
    ply = ply,
    moves = moves,
    color = color,
    date = DateTime.now,
    attempts = 0,
    score = 50)

  import reactivemongo.bson._
  import lila.db.BSON
  import BSON.BSONJodaDateTimeHandler

  implicit val moveBSONHandler = new BSON[Move] {

    def reads(r: BSON.Reader): Move = Move(
      first = r str "first",
      cp = r int "cp",
      line = r str "line" split ' ' toList)

    def writes(w: BSON.Writer, o: Move) = BSONDocument(
      "first" -> o.first,
      "cp" -> o.cp,
      "line" -> o.line.mkString(" "))
  }

  object BSONFields {
    val id = "_id"
    val fen = "fen"
    val ply = "ply"
    val moves = "moves"
    val white = "white"
    val date = "date"
    val attempts = "attempts"
    val score = "score"
  }

  implicit val openingBSONHandler = new BSON[Opening] {

    import BSONFields._

    def reads(r: BSON.Reader): Opening = Opening(
      id = r int id,
      fen = r str fen,
      ply = r int ply,
      moves = r.get[List[Move]](moves),
      color = Color(r bool white),
      date = r date date,
      attempts = r int attempts,
      score = r double score)

    def writes(w: BSON.Writer, o: Opening) = BSONDocument(
      id -> o.id,
      fen -> o.fen,
      ply -> o.ply,
      moves -> o.moves,
      white -> o.color.white,
      date -> o.date,
      attempts -> o.attempts,
      score -> o.score)
  }
}

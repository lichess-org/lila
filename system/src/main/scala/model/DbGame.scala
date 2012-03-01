package lila.system
package model

import com.novus.salat.annotations._
import com.mongodb.casbah.Imports._

import lila.chess._
import Pos._

case class DbGame(
    @Key("_id") id: String,
    players: List[Player],
    pgn: String,
    status: Int,
    turns: Int,
    variant: Int) {

  def toChess = {

    def posPiece(posCode: Char, roleCode: Char, color: Color): Option[(Pos, Piece)] = for {
      pos ← Piotr.decodePos get posCode
      role ← Piotr.decodeRole get roleCode
    } yield (pos, Piece(color, role))

    Game(
      board = Board(
        (for {
          player ← players
          color = Color.allByName(player.color)
          piece ← player.ps.split(' ').toList
        } yield piece.toList match {
          case pos :: role :: Nil  ⇒ posPiece(pos, role, color)
          case pos :: role :: rest ⇒ posPiece(pos, role, color)
          case _ ⇒ None
        }).flatten toMap,
        History()
      ),
      player = if (0 == turns % 2) White else Black,
      pgnMoves = pgn
    )
  }
  //foreach(explode(' ', $this->ps) as $p) {
  //$class = $baseClass . Piece::letterToClass(strtolower($p{1}));
  //$pos = Board::keyToPos(Board::piotrToKey($p{0}));
  //$piece = new $class($pos[0], $pos[1]);
  //if (ctype_upper($p{1})) $piece->setIsDead(true);
  //$meta = substr($p, 2);
  //if (is_numeric($meta)) {
  //$piece->setFirstMove((int)$meta);
  //}
  //$pieces[] = $piece;
  //}
}

package lila.chess
package model

case class Game(
    board: Board,
    player: Color,
    pgnMoves: List[String] = Nil) {

  def this() = this(Board.empty, White)

  //def playMoves(moves: (Pos, Pos)*): Valid[Game] =
    //moves.foldLeft(success(this): Valid[Situation]) { (sit, move) ⇒
      //sit flatMap { s ⇒ s.playMove(move._1, move._2) }
    //}

  //def playMove(from: Pos, to: Pos, promotion: PromotableRole = Queen): Valid[Game] = {
  //}

  val players = List(White, Black)

  def situation = board as player
}

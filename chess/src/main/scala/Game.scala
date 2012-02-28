package lila.chess

case class Game(
    board: Board,
    player: Color,
    pgnMoves: List[String] = Nil) {

  def this() = this(Board.empty, White)

  def playMoves(moves: (Pos, Pos)*): Valid[Game] =
    moves.foldLeft(success(this): Valid[Game]) { (sit, move) ⇒
      sit flatMap { s ⇒ s.playMove(move._1, move._2) }
    }

  def playMove(from: Pos, to: Pos, promotion: PromotableRole = Queen): Valid[Game] =
    situation.playMove(from, to, promotion) map { move =>
      copy(board = move.after, player = !player)
    }

  val players = List(White, Black)

  def situation = board as player

  def as(c: Color) = copy(player = c)
}

object Game {

  def apply(): Game = Game(
    board = Board(),
    player = White)
}

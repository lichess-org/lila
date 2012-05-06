package lila.chess

class Replay private[chess] (val game: Game, val moves: List[Move]) {

}

object Replay {

  def apply(game: Game) = new Replay(game, Nil)
}

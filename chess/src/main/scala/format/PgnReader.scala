package lila.chess
package format

object PgnReader {

  def apply(pgn: String): Valid[Replay] =
    PgnParser(pgn) flatMap { parsed ⇒
      parsed.sans.foldLeft(Replay(makeGame(parsed.tags)).success: Valid[Replay]) {
        case (replayValid, san) ⇒ for {
          replay ← replayValid
          move ← san(replay.game)
        } yield new Replay(
          game = replay.game(move),
          moves = move :: replay.moves)
      }
    }

  def makeGame(tags: List[Tag]): Game =
    tags collect {
      case Fen(fen) ⇒ fen
    } match {
      case Nil ⇒ Game()
      case fen :: _ ⇒ (Forsyth << fen).fold(
        situation ⇒ Game(board = situation.board, player = situation.color),
        Game()
      )
    }
}

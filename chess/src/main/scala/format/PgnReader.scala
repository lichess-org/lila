package lila.chess
package format

object PgnReader {

  def apply(pgn: String, tags: List[Tag] = Nil): Valid[Replay] = 
    withSans(pgn, identity, tags)

  def withSans(
    pgn: String, 
    op: List[San] ⇒ List[San],
    tags: List[Tag] = Nil): Valid[Replay] = for {
    parsed ← PgnParser(pgn)
    game ← makeGame(parsed.tags ::: tags)
    replay ← op(parsed.sans).foldLeft(Replay(game).success: Valid[Replay]) {
      case (replayValid, san) ⇒ for {
        replay ← replayValid
        move ← san(replay.game)
      } yield new Replay(
        game = replay game move,
        moves = move :: replay.moves)
    }
  } yield replay

  def makeGame(tags: List[Tag]): Valid[Game] =
    tags collect {
      case Fen(fen) ⇒ fen
    } match {
      case Nil ⇒ Game().success
      case fen :: Nil ⇒ (Forsyth << fen).fold(
        s ⇒ Game(board = s.board, player = s.color).success,
        "Invalid fen %s".format(fen).failNel
      )
      case many ⇒ "Multiple fen tags".failNel
    }
}

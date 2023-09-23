package lila.relay

import lila.study.*
import chess.format.Fen

/* Try to detect several ways for the input to be wrong */
private object RelayInputSanity:

  enum Fail(val msg: String):
    case Missing(pos: Int) extends Fail(s"Missing game for Chapter ${pos + 1}")
    case Misplaced(gamePos: Int, chapterPos: Int)
        extends Fail(s"Game ${gamePos + 1} matches with Chapter ${chapterPos + 1}")

  def apply(chapters: List[Chapter], games: RelayGames): Either[Fail, RelayGames] = {
    if chapters.isEmpty then Right(games)
    else if isValidTCEC(chapters, games) then Right(games)
    else
      val relayChapters: List[RelayChapter] = chapters.flatMap: chapter =>
        chapter.relay map chapter.->
      detectMissingOrMisplaced(relayChapters, games) toLeft games
  } map fixDgtKingsInTheCenter

  private type RelayChapter = (Chapter, Chapter.Relay)

  private def detectMissingOrMisplaced(chapters: List[RelayChapter], games: Vector[RelayGame]): Option[Fail] =
    chapters
      .flatMap: (chapter, relay) =>
        games.lift(relay.index) match
          case None => Fail.Missing(relay.index).some
          case Some(game) if !game.playerTagsMatch(chapter.tags) =>
            games.zipWithIndex.collectFirst:
              case (otherGame, otherPos) if otherGame playerTagsMatch chapter.tags =>
                Fail.Misplaced(otherPos, relay.index)
          case _ => None
      .headOption

  // TCEC style has one game per file, and reuses the file for all games
  private def isValidTCEC(chapters: List[Chapter], games: RelayGames) =
    games match
      case Vector(onlyGame) =>
        chapters.lastOption.exists: c =>
          onlyGame staticTagsMatch c.tags
      case _ => false

  // DGT puts the kings in the center on game end
  // and sends it as actual moves if the kings were close to the center
  // so we need to remove the boggus king moves
  private def fixDgtKingsInTheCenter(games: RelayGames): RelayGames = games map { game =>
    game.copy(
      root = game.root.takeMainlineWhile: node =>
        !dgtBoggusKingMoveRegex.matches(node.move.san.value) ||
          !Fen.read(game.variant, node.fen).forall { sit =>
            sit.board.checkOf(!sit.color).yes // the king that moved is in check
          }
    )
  }
  private val dgtBoggusKingMoveRegex = """^K[de][45]""".r

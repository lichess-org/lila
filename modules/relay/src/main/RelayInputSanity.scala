package lila.relay

import lila.study._

/* Try and detect variant ways for the input to be wrong */
private object RelayInputSanity {

  sealed abstract class Fail(val msg: String)
  case class Missing(pos: Int) extends Fail(s"Missing game for Chapter ${pos + 1}")
  case class Misplaced(gamePos: Int, chapterPos: Int)
      extends Fail(s"Game ${gamePos + 1} matches with Chapter ${chapterPos + 1}")

  def apply(chapters: List[Chapter], games: RelayGames): Option[Fail] =
    if (chapters.isEmpty) none
    else if (isValidTCEC(chapters, games)) none
    else {
      val relayChapters: List[RelayChapter] = chapters.flatMap { chapter =>
        chapter.relay map chapter.->
      }
      detectMissingOrMisplaced(relayChapters, games)
    }

  private def detectMissingOrMisplaced(chapters: List[RelayChapter], games: Vector[RelayGame]): Option[Fail] =
    chapters flatMap {
      case (chapter, relay) =>
        games.lift(relay.index) match {
          case None => Missing(relay.index).some
          case Some(game) if !game.staticTagsMatch(chapter) =>
            games.zipWithIndex collectFirst {
              case (otherGame, otherPos) if otherGame staticTagsMatch chapter =>
                Misplaced(otherPos, relay.index)
            }
          case _ => None
        }
    } headOption

  // TCEC style has one game per file, and reuses the file for all games
  private def isValidTCEC(chapters: List[Chapter], games: RelayGames) =
    games match {
      case Vector(onlyGame) =>
        chapters.lastOption.exists { c =>
          onlyGame staticTagsMatch c.tags
        }
      case _ => false
    }

  private type RelayChapter = (Chapter, Chapter.Relay)
}

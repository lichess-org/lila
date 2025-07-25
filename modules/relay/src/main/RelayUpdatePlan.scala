package lila.relay

import lila.study.Chapter
import chess.format.pgn.Tags
import lila.tree.Node

object RelayUpdatePlan:

  case class Input(chapters: List[Chapter], games: RelayGames):
    override def toString: String =
      s"Input(chapters = ${chapters.map(_.name)}, games = ${games.map(_.tags.names)})"

  case class Plan(
      input: Input,
      reorder: Option[List[StudyChapterId]],
      update: List[(Chapter, RelayGame)],
      append: RelayGames,
      orphans: List[Chapter] // existing chapters that don't match any of the input games
  ):
    def isJustInitialChapterUpdate =
      input.chapters.sizeIs == 1 &&
        input.chapters.headOption.forall(_.isEmptyInitial) &&
        update.sizeIs == 1 && append.isEmpty && orphans.isEmpty

    override def toString: String =
      s"Output(reorder = $reorder, update = ${update.map(_._1.name)}, append = ${append.map(_.tags.names)})"

  def apply(chapters: List[Chapter], games: RelayGames): Plan =
    apply(Input(chapters, games))

  def apply(input: Input): Plan =
    import input.*

    val likelyMatches: List[(RelayGame, Chapter)] =
      games
        .foldLeft(List.empty[(RelayGame, Chapter)]): (matches, game) =>
          chapters
            .collectFirst:
              case chapter if isSameGame(game, chapter) && !matches.exists(_._2 == chapter) =>
                game -> chapter
            .fold(matches)(_ :: matches)
        .reverse

    val replaceInitialChapter: Option[(RelayGame, Chapter)] =
      chapters match
        case List(only) if only.isEmptyInitial =>
          // likelyMatches should be empty
          games.headOption.map(_ -> only)
        case _ => none

    val updates: List[(Chapter, RelayGame)] = replaceInitialChapter match
      case Some(initial) => List(initial.swap)
      case None => likelyMatches.map(_.swap)

    val appends: Vector[RelayGame] = games.filterNot: g =>
      updates.exists(_._2 == g)

    // requires all chapters to be updated
    // contains all chapter ids
    val reorder: Option[List[StudyChapterId]] =
      val ids = updates.map(_._1.id)
      Option.when(ids.size == chapters.size && ids != chapters.map(_.id))(ids)

    val orphans: List[Chapter] =
      val updatedIds = updates.view.map(_._1.id).toSet
      chapters.filterNot(c => updatedIds.contains(c.id))

    Plan(
      input = input,
      reorder = reorder,
      update = updates,
      append = appends,
      orphans = orphans
    )

  private[relay] def isSameGame(game: RelayGame, chapter: Chapter): Boolean =
    isSameGameBasedOnTags(game.tags, chapter.tags) || isSameGameBasedOnTagsAndFirstMoves(game, chapter)

  // We don't use tags.boardNumber.
  // Organizers change it at any time while reordering the boards.
  private[relay] def isSameGameBasedOnTags(gameTags: Tags, chapterTags: Tags): Boolean =
    ~(gameTags(_.GameId), chapterTags(_.GameId)).mapN(_ == _) || {
      allSame(RelayGame.eventTags)(gameTags, chapterTags) &&
      gameTags.roundNumber == chapterTags.roundNumber &&
      gameTags.boardNumber == chapterTags.boardNumber &&
      playerTagsMatch(gameTags, chapterTags)
    }

  // if the board number has changed, but most moves look similar,
  // then probably it's the same game but the source changed the order and board numbers
  private[relay] def isSameGameBasedOnTagsAndFirstMoves(game: RelayGame, chapter: Chapter): Boolean =
    allSame(RelayGame.eventTags)(game.tags, chapter.tags) &&
      game.tags.roundNumber == chapter.tags.roundNumber && {
        val gameMoves = game.root.mainlineNodeList
        val chapterMoves = chapter.root.mainlineNodeList
        sameFirstMoves(gameMoves, chapterMoves)
      }

  private[relay] def sameFirstMoves(game: List[Node], chapter: List[Node]): Boolean =
    val maxMoves = Math.min(game.size, chapter.size)
    val checkMoveAt = Math.max(0, maxMoves - 1)
    val found = for
      g <- game.lift(checkMoveAt)
      c <- chapter.lift(checkMoveAt)
    yield g.fen == c.fen
    ~found

  private def playerTagsMatch(gameTags: Tags, chapterTags: Tags): Boolean =
    val bothHaveFideIds = List(gameTags, chapterTags).forall: ts =>
      RelayGame.fideIdTags.forall(side => ts(side).exists(_ != "0"))
    if bothHaveFideIds
    then allSame(RelayGame.fideIdTags)(gameTags, chapterTags)
    else allSame(RelayGame.nameTags)(gameTags, chapterTags)

  private def allSame(tagNames: RelayGame.TagNames)(gameTags: Tags, chapterTags: Tags) = tagNames.forall:
    tag => gameTags(tag) == chapterTags(tag)

package lila.relay

import lila.study.Chapter
import chess.format.pgn.Tags

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

    val tagMatches: List[(RelayGame, Chapter)] =
      games
        .foldLeft(List.empty[(RelayGame, Chapter)]): (matches, game) =>
          chapters
            .collectFirst:
              case chapter if isSameGame(game.tags, chapter.tags) && !matches.exists(_._2 == chapter) =>
                game -> chapter
            .fold(matches)(_ :: matches)
        .reverse

    val replaceInitialChapter: Option[(RelayGame, Chapter)] =
      chapters match
        case List(only) if only.isEmptyInitial =>
          // tagMatches should be empty
          games.headOption.map(_ -> only)
        case _ => none

    val updates: List[(Chapter, RelayGame)] = replaceInitialChapter match
      case Some(initial) => List(initial.swap)
      case None          => tagMatches.map(_.swap)

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

  // We don't use tags.boardNumber.
  // Organizers change it at any time while reordering the boards.
  private[relay] def isSameGame(gameTags: Tags, chapterTags: Tags): Boolean =

    def isSameLichessGame = ~(gameTags(_.GameId), chapterTags(_.GameId)).mapN(_ == _)

    def playerTagsMatch: Boolean =
      val bothHaveFideIds = List(gameTags, chapterTags).forall: ts =>
        RelayGame.fideIdTags.forall(side => ts(side).exists(_ != "0"))
      if bothHaveFideIds
      then allSame(RelayGame.fideIdTags)
      else allSame(RelayGame.nameTags)

    def allSame(tagNames: RelayGame.TagNames) = tagNames.forall: tag =>
      gameTags(tag) == chapterTags(tag)

    isSameLichessGame || {
      allSame(RelayGame.eventTags) &&
      gameTags.roundNumber == chapterTags.roundNumber &&
      gameTags.boardNumber == chapterTags.boardNumber &&
      playerTagsMatch
    }

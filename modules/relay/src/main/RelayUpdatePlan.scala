package lila.relay

import lila.study.Chapter
import lila.study.Chapter.Order

object RelayUpdatePlan:

  case class Input(chapters: List[Chapter], games: RelayGames)
  case class Output(
      reorder: Option[List[StudyChapterId]],
      update: List[(Chapter, RelayGame)],
      append: RelayGames,
      delete: List[StudyChapterId]
  )
  case class Plan(input: Input, output: Output)

  def apply(chapters: List[Chapter], games: RelayGames): Plan =
    apply(Input(chapters, games))

  def apply(input: Input): Plan =
    import input.*

    val tagMatches: List[(RelayGame, Chapter)] =
      games
        .flatMap: game =>
          chapters.collect:
            case chapter if game.staticTagsMatch(chapter.tags) => game -> chapter
        .toList

    val updates: List[(Chapter, RelayGame)] = tagMatches.map(_.swap)

    val appends = games.filterNot: g =>
      tagMatches.exists(_._1 == g)

    // chapters:  A B C D
    // games:     C B
    // reorder:   ???
    // requires all chapters to be updated
    // contains all chapter ids
    val reorder: Option[List[StudyChapterId]] =
      val ids = updates.map(_._1.id)
      Option.when(ids.size == chapters.size && ids != chapters.map(_.id))(ids)

    val deletes: List[StudyChapterId] =
      (updates.isEmpty && appends.nonEmpty).so:
        chapters match
          case List(only) if only.isEmptyInitial => List(only.id)
          case _                                 => Nil

    val output = Output(
      reorder = reorder,
      update = updates,
      append = appends,
      delete = deletes
    )
    Plan(input, output)

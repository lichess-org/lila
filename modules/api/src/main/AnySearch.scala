package lila.api

import lila.core.id.*

final class AnySearch(
    gameEnv: lila.game.Env,
    relayEnv: lila.relay.Env,
    studyEnv: lila.study.Env,
    puzzleEnv: lila.puzzle.Env,
    ublogApi: lila.ublog.UblogApi
)(using Executor):

  private val idRegex = """^[a-zA-Z0-9]{4,12}$""".r

  def redirect(str: String): Fu[Option[String]] =
    str.trim.some
      .filter(idRegex.matches)
      .so: id =>

        def game = gameEnv.gameRepo.exists(GameId(id)).map(_.option(s"/$id"))

        def broadcastRound = relayEnv.api.byIdWithTour(RelayRoundId(id)).map2(_.path)
        def broadcastTour = relayEnv.api.tourById(RelayTourId(id)).map2(_.path)

        def study = studyEnv.studyRepo.exists(StudyId(id)).map(_.option(routes.Study.show(StudyId(id)).url))
        def chapter =
          studyEnv.chapterRepo.byId(StudyChapterId(id)).map2(c => routes.Study.chapter(c.studyId, c.id).url)

        def puzzle = puzzleEnv.api.puzzle.find(PuzzleId(id)).map2(_ => routes.Puzzle.show(id).url)

        def ublog = ublogApi.getPost(UblogPostId(id)).map2(_ => routes.Ublog.redirect(UblogPostId(id)).url)

        game
          .orElse(broadcastRound)
          .orElse(broadcastTour)
          .orElse(study)
          .orElse(chapter)
          .orElse(puzzle)
          .orElse(ublog)

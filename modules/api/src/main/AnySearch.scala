package lila.api

import lila.core.id.*

final class AnySearch(
    gameEnv: lila.game.Env,
    relayEnv: lila.relay.Env,
    studyEnv: lila.study.Env,
    puzzleEnv: lila.puzzle.Env,
    tourEnv: lila.tournament.Env,
    swissEnv: lila.swiss.Env,
    ublogApi: lila.ublog.UblogApi,
    teamEnv: lila.team.Env,
    fideEnv: lila.fide.Env
)(using Executor):

  private val idRegex = """^[a-zA-Z0-9]{4,12}$""".r

  def redirect(str: String): Fu[Option[String]] =
    str.trim.some
      .filter(idRegex.matches)
      .so: id =>
        def game = gameEnv.gameRepo.exists(GameId(id)).map(_.option(s"/$id"))

        def broadcastRound = relayEnv.api.byIdWithTour(RelayRoundId(id)).map2(_.path)
        def broadcastTour = relayEnv.api.tourById(RelayTourId(id)).map2(_.call.url)

        def study = studyEnv.studyRepo.exists(StudyId(id)).map(_.option(routes.Study.show(StudyId(id)).url))
        def chapter =
          studyEnv.chapterRepo.byId(StudyChapterId(id)).map2(c => routes.Study.chapter(c.studyId, c.id).url)

        def puzzle = puzzleEnv.api.puzzle.find(PuzzleId(id)).map2(_ => routes.Puzzle.show(id).url)

        def tour = tourEnv.api.get(TourId(id)).map2(_ => routes.Tournament.show(TourId(id)).url)

        def swiss = swissEnv.api.fetchByIdNoCache(SwissId(id)).map2(_ => routes.Swiss.show(SwissId(id)).url)

        def ublog = ublogApi.getPost(UblogPostId(id)).map2(_ => routes.Ublog.redirect(UblogPostId(id)).url)

        def team = teamEnv.teamRepo.enabled(TeamId(id)).map2(_ => routes.Team.show(TeamId(id)).url)

        def fideplayer = chess.FideId
          .from(str.toIntOption)
          .so(id => fideEnv.playerApi.fetch(id).map2(p => routes.Fide.show(id, p.slug).url))

        game
          .orElse(broadcastRound)
          .orElse(broadcastTour)
          .orElse(study)
          .orElse(chapter)
          .orElse(puzzle)
          .orElse(tour)
          .orElse(swiss)
          .orElse(ublog)
          .orElse(team)
          .orElse(fideplayer)

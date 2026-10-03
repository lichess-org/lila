package lila.api

import alleycats.Zero
import play.api.mvc.RequestHeader

import lila.core.id.*
import lila.core.security.IsProxy
import lila.common.HTTPRequest
import lila.mon.extensions.*

final class AnySearch(
    gameEnv: lila.game.Env,
    relayEnv: lila.relay.Env,
    studyEnv: lila.study.Env,
    puzzleEnv: lila.puzzle.Env,
    tourEnv: lila.tournament.Env,
    swissEnv: lila.swiss.Env,
    ublogApi: lila.ublog.UblogApi,
    teamEnv: lila.team.Env,
    fideEnv: lila.fide.Env,
    baseUrl: lila.core.config.BaseUrl
)(using Executor):

  private val idRegex = """^[a-zA-Z0-9]{4,12}$""".r

  private def sameReferrer(req: RequestHeader): Boolean =
    HTTPRequest.referer(req).exists(_.startsWith(baseUrl.value))

  def redirect(str: String, proxy: Option[IsProxy])(using ctx: Context): Fu[Option[String]] =
    (ctx.isAuth || (!proxy.exists(_.couldBeEnum) && sameReferrer(ctx.req)))
      .option(str.trim)
      .filter(idRegex.matches)
      .so: id =>
        val idLike = str.forall(_.isLetterOrDigit)
        def find[ID, A: Zero](f: ID => Fu[A], size: Int = 8)(using sr: SameRuntime[String, ID]): Fu[A] =
          (idLike && str.length == size).so(f(sr(id)))

        def game = find(gameEnv.gameRepo.exists).map(_.option(s"/$id"))

        def broadcastRound = find(relayEnv.api.byIdWithTour).map2(_.path)
        def broadcastTour = find(relayEnv.api.tourById).map2(_.call.url)
        def broadcastGroup = find(relayEnv.api.groupById).map2(_.call.url)

        def study = find(studyEnv.studyRepo.exists).map(_.option(routes.Study.show(StudyId(id)).url))
        def chapter = find(studyEnv.chapterRepo.byId).map2(c => routes.Study.chapter(c.studyId, c.id).url)

        def puzzle = find(puzzleEnv.api.puzzle.find, 5).map2(_ => routes.Puzzle.show(id).url)

        def tour = find(tourEnv.api.get).map2(t => routes.Tournament.show(t.id).url)

        def swiss = find(swissEnv.api.fetchByIdNoCache).map2(s => routes.Swiss.show(s.id).url)

        def ublog = find(ublogApi.getPost).map2(p => routes.Ublog.redirect(p.id).url)

        def team = teamEnv.teamRepo.enabled(TeamId(str)).map2(t => routes.Team.show(t.id).url)

        def fideplayer = chess.FideId
          .from(str.toIntOption)
          .so(id => fideEnv.playerApi.fetch(id).map2(p => routes.Fide.show(id, p.slug).url))

        game
          .orElse(broadcastRound)
          .orElse(broadcastTour)
          .orElse(broadcastGroup)
          .orElse(study)
          .orElse(chapter)
          .orElse(puzzle)
          .orElse(tour)
          .orElse(swiss)
          .orElse(ublog)
          .orElse(team)
          .orElse(fideplayer)
      .monValue(res => lila.mon.anySearch.time(res.isDefined, ctx.isAuth))

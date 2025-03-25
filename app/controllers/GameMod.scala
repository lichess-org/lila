package controllers
import lila.api.GameApiV2
import lila.app.{ *, given }

final class GameMod(env: Env)(using akka.stream.Materializer) extends LilaController(env):

  import lila.mod.GameMod.*

  def index(username: UserStr) = SecureBody(_.GamesModView) { ctx ?=> _ ?=>
    Found(meOrFetch(username)): user =>
      val form   = filterForm.bindFromRequest()
      val filter = form.fold(_ => emptyFilter, identity)
      for
        arenas  <- env.tournament.leaderboardApi.recentByUser(user, 1)
        swisses <- env.activity.read.recentSwissRanks(user.id)
        povs    <- fetchGames(user, filter)
        games <-
          if isGranted(_.UserEvaluate)
          then env.mod.assessApi.makeAndGetFullOrBasicsFor(povs).map(Right.apply)
          else fuccess(Left(povs))
        page <- renderPage(views.mod.games(user, form, games, arenas.currentPageResults, swisses))
      yield Ok(page)
  }

  private def fetchGames(user: lila.user.User, filter: Filter) =
    val select = toDbSelect(user, filter) ++ lila.game.Query.finished
    import akka.stream.scaladsl.*
    env.game.gameRepo
      .recentGamesFromSecondaryCursor(select)
      .documentSource(10_000)
      .filter: game =>
        filter.perf.forall(game.perfKey ==)
      .take(filter.nbGames)
      .mapConcat { Pov(_, user).toList }
      .toMat(Sink.seq)(Keep.right)
      .run()
      .map(_.toList)

  def post(username: UserStr) = SecureBody(_.GamesModView) { ctx ?=> me ?=>
    Found(meOrFetch(username)): user =>
      bindForm(actionForm)(
        err => BadRequest(err.toString),
        {
          case (gameIds, Some("pgn")) => downloadPgn(user, gameIds)
          case (gameIds, Some("analyse") | None) if isGranted(_.UserEvaluate) =>
            multipleAnalysis(me, gameIds)
          case _ => notFound
        }
      )
  }

  private def multipleAnalysis(me: Me, gameIds: Seq[GameId])(using Context) =
    for
      games <- env.game.gameRepo.unanalysedGames(gameIds)
      _ <- games.sequentiallyVoid: game =>
        env.fishnet
          .analyser(
            game,
            lila.fishnet.Work.Sender(
              userId = me,
              ip = ctx.ip.some,
              mod = true,
              system = false
            ),
            lila.fishnet.Work.Origin.autoHunter.some
          )
      _ <- env.fishnet.awaiter(games.map(_.id), 2.minutes)
    yield NoContent

  private def downloadPgn(user: lila.user.User, gameIds: Seq[GameId])(using Context) =
    val res = Ok.chunked:
      env.api.gameApiV2.exportByIds(
        GameApiV2.ByIdsConfig(
          ids = gameIds,
          format = GameApiV2.Format.PGN,
          flags = lila.game.PgnDump.WithFlags(),
          perSecond = MaxPerSecond(100),
          playerFile = none
        )
      )
    asAttachmentStream(s"lichess_mod_${user.username}_${gameIds.size}_games.pgn")(res).as(pgnContentType)

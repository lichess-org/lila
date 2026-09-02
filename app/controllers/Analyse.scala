package controllers

import chess.{ Division, Ply }
import chess.format.Fen
import chess.format.pgn.SanStr
import chess.variant.Variant
import chess.json.Json.given
import play.api.libs.json.*
import play.api.mvc.*

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.core.misc.lpv.LpvEmbed
import lila.game.PgnDump
import lila.oauth.AccessToken
import lila.study.Study.WithChapter
import lila.tree.{ ExportOptions, Analysis }

final class Analyse(
    env: Env,
    gameC: => Game,
    roundC: => Round
) extends LilaController(env):

  def requestAnalysis(id: GameId) = AuthOrScoped(_.Web.Mobile) { ctx ?=> me ?=>
    Found(env.game.gameRepo.game(id)): game =>
      env.fishnet
        .analyser(
          game,
          lila.fishnet.Work.Sender(
            userId = me,
            ip = ctx.ip.some,
            mod = isGranted(_.UserEvaluate) || isGranted(_.Relay),
            system = false
          )
        )
        .map:
          _.error.fold(NoContent)(BadRequest(_))
  }

  private[controllers] def replay(pov: Pov, userTv: Option[lila.user.User])(using ctx: Context) =
    if ctx.req.client.isCrawler then replayForCrawler(pov)
    else
      for
        initialFen <- env.game.gameRepo.initialFen(pov.game)
        users <- env.user.api.gamePlayers(pov.game.players.map(_.userId), pov.game.perfKey)
        _ = gameC.preloadUsers(users)
        res <- RedirectAtFen(pov, initialFen):
          val pgnFlags = PgnDump.WithFlags(
            clocks = false,
            rating = ctx.pref.showRatings,
            opening = ctx.isAuth.option(true)
          )
          val opening = pgnFlags.opening.so(env.game.gameOpening.atPly(pov.game, _))
          (
            env.analyse.analyser.get(pov.game),
            pov.game.metadata.analysed.not.so(env.fishnet.api.userAnalysisExists(Analysis.Id(pov.gameId))),
            pov.game.simulId.so(env.simul.repo.find),
            roundC.getWatcherChat(pov.game),
            ctx.noBlind.so(env.game.crosstableApi.withMatchup(pov.game)),
            env.bookmark.api.exists(pov.game, ctx.me),
            env.api.pgnDump(
              pov.game,
              initialFen,
              analysis = none,
              opening = opening,
              pgnFlags
            )
          ).flatMapN: (analysis, analysisInProgress, simul, chat, crosstable, bookmarked, pgn) =>
            env.api.roundApi
              .review(
                pov,
                users,
                analysis,
                opening.map(_.opening),
                initialFen = initialFen,
                tv = userTv.map: u =>
                  lila.round.OnTv.User(u.id),
                withFlags = ExportOptions(
                  movetimes = true,
                  clocks = true,
                  division = true,
                  rating = ctx.pref.showRatings,
                  lichobileCompat = HTTPRequest.isLichobile(ctx.req),
                  puzzles = true
                )
              )
              .flatMap: data =>
                Ok.page(
                  views.analyse.replay.forBrowser(
                    pov,
                    data,
                    initialFen,
                    env.analyse.annotator(pgn, pov.game, analysis, opening).render,
                    analysis,
                    analysisInProgress,
                    simul,
                    crosstable,
                    userTv,
                    chat,
                    bookmarked = bookmarked
                  )
                ).map(_.enforceCrossSiteIsolation)
      yield res

  def embed(gameId: GameId, color: Color) = embedReplayGame(gameId, color)

  val AcceptsPgn = Accepting("application/x-chess-pgn")

  def embedReplayGame(gameId: GameId, color: Color) = Anon:
    InEmbedContext:
      env.api.textLpvExpand
        .getPgn(gameId)
        .map:
          case Some(LpvEmbed.PublicPgn(pgn)) =>
            render:
              case AcceptsPgn() => Ok(pgn)
              case _ =>
                Ok.snip:
                  views.analyse.embed.lpv(
                    pgn,
                    getPgn = true,
                    title = "Lichess PGN viewer",
                    Json.obj("orientation" -> color.name)
                  )
          case _ =>
            render:
              case AcceptsPgn() => NotFound("*")
              case _ => NotFound.snip(views.analyse.embed.notFound)

  private def RedirectAtFen(pov: Pov, initialFen: Option[Fen.Full])(or: => Fu[Result])(using
      Context
  ): Fu[Result] =
    (get("fen").map(Fen.Full.clean): Option[Fen.Full]).fold(or): atFen =>
      val url = routes.Round.watcher(pov.gameId, pov.color)
      chess.Replay
        .plyAtFen(pov.game.sans, initialFen, pov.game.variant, atFen)
        .fold(
          _ => Redirect(url),
          ply => Redirect(s"$url#$ply")
        )

  private def replayForCrawler(pov: Pov)(using Context) = for
    initialFen <- env.game.gameRepo.initialFen(pov.game)
    analysis <- env.analyse.analyser.get(pov.game)
    simul <- pov.game.simulId.so(env.simul.repo.find)
    crosstable <- env.game.crosstableApi.withMatchup(pov.game)
    pgn <- env.api.pgnDump(pov.game, initialFen, analysis, none, PgnDump.WithFlags(clocks = false))
    page <- renderPage:
      views.analyse.replay.forCrawler(
        pov,
        initialFen,
        env.analyse.annotator(pgn, pov.game, analysis, none).render,
        simul,
        crosstable
      )
  yield Ok(page)

  def externalEngineList = ScopedBody(_.Engine.Read) { _ ?=> me ?=>
    env.analyse.externalEngine.list(me).map { list =>
      JsonOk(JsArray(list.map(lila.analyse.ExternalEngine.jsonWrites.writes)))
    }
  }

  def externalEngineShow(id: String) = ScopedBody(_.Engine.Read) { _ ?=> me ?=>
    Found(env.analyse.externalEngine.find(me, id)): engine =>
      JsonOk(lila.analyse.ExternalEngine.jsonWrites.writes(engine))
  }

  def externalEngineCreate = ScopedBody(_.Engine.Write) { ctx ?=> me ?=>
    HTTPRequest.bearer.so: (bearer, _) =>
      val tokenId = AccessToken.idFrom(bearer)
      bindForm(lila.analyse.ExternalEngine.form)(
        jsonFormError,
        data =>
          env.analyse.externalEngine.create(me, data, tokenId).map { engine =>
            Created(lila.analyse.ExternalEngine.jsonWrites.writes(engine))
          }
      )
  }

  def externalEngineUpdate(id: String) = ScopedBody(_.Engine.Write) { ctx ?=> me ?=>
    Found(env.analyse.externalEngine.find(me, id)): engine =>
      bindForm(lila.analyse.ExternalEngine.form)(
        jsonFormError,
        data =>
          env.analyse.externalEngine.update(engine, data).map { engine =>
            JsonOk(lila.analyse.ExternalEngine.jsonWrites.writes(engine))
          }
      )
  }

  def externalEngineDelete(id: String) = AuthOrScoped(_.Engine.Write) { _ ?=> me ?=>
    env.analyse.externalEngine.delete(me, id).elseNotFound(jsonOkResult)
  }

  private def WithStudyContributor(id: Analysis.Id)(
      f: lila.study.Chapter => Fu[Result]
  )(using Context, Me): Fu[Result] = id match
    case Analysis.Id.Study(studyId, chapterId) =>
      Found(env.study.api.byIdWithChapter(studyId, chapterId)):
        case WithChapter(study, chapter) =>
          if study.canContribute(summon[Me]) then f(chapter) else forbiddenJson()
    case Analysis.Id.Game(_) => fuccess(BadRequest("Study analysis required"))

  def postAnalysisXhr = AuthBody(parse.json) { ctx ?=> me ?=>
    ctx.body.body.validate[lila.analyse.Analysis] match
      case JsError(errs) => fuccess(BadRequest(errs.mkString("\n")))
      case JsSuccess(uploaded, _) =>
        WithStudyContributor(uploaded.id): chapter =>
          val moves = chess.format
            .UciDump(
              moves = chapter.root.mainline.map(_.move.san),
              initialFen = chapter.root.fen.some,
              variant = chapter.setup.variant
            )
            .toOption
            .map(_.flatMap(chess.format.Uci.apply).map(_.uci).mkString(" ")) | ""
          for
            requested <- env.fishnet.api.userAnalysisExists(uploaded.id)
            result <-
              if requested then fuccess(Locked)
              else
                env.analyse.analyser
                  .save(
                    uploaded,
                    (() => Analysis.positionHash(chapter.setup.variant, chapter.root.fen.some, moves)).some
                  )
                  .inject(Ok)
          yield result
  }

  def deleteAnalysisXhr(studyId: StudyId, chapterId: StudyChapterId) = Auth { _ ?=> me ?=>
    WithStudyContributor(Analysis.Id(studyId, chapterId)): _ =>
      env.study.serverEvalMerger.remove(studyId, chapterId).inject(NoContent)
  }

  def divisionXhr = OpenBodyOf(parse.json): ctx ?=>
    val json = ctx.body.body
    val parsed = for
      variantKey <- (json \ "variant").validate[String]
      variant <- Variant(Variant.LilaKey(variantKey)).fold[JsResult[Variant]](
        JsError(s"Invalid variant: $variantKey")
      )(JsSuccess(_))
      initialFen <- (json \ "initialFen")
        .validateOpt[String]
        .map(_.map(fen => Fen.Full.clean(fen): Fen.Full))
      sans <- (json \ "moves").validate[Vector[String]].map(_.map(SanStr(_)))
    yield env.game.divider(sans, variant, initialFen)
    parsed.fold(errs => BadRequest(errs.mkString("\n")).toFuccess, JsonOk(_))

  def reviewXhr = OpenBodyOf(parse.json): ctx ?=>
    val json = ctx.body.body
    val parsed = for
      analysis <- (json \ "analysis").validate[lila.analyse.Analysis]
      middle <- (json \ "division" \ "middle").validateOpt[Int]
      end <- (json \ "division" \ "end").validateOpt[Int]
    yield
      val division = Division(middle.map(Ply(_)), end.map(Ply(_)), Ply.initial)
      val absoluteDivision = division.copy(
        middle = division.middle.map(_ + analysis.startPly),
        end = division.end.map(_ + analysis.startPly)
      )
      Json.obj(
        "summary" -> env.analyse.jsonView.bothPlayers(
          analysis.startPly,
          analysis,
          division = absoluteDivision
        ),
        "moves" -> env.analyse.jsonView.moves(analysis)
      )
    parsed.fold(errs => BadRequest(errs.mkString("\n")).toFuccess, JsonOk(_))

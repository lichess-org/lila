package controllers

import chess.format.Fen
import play.api.libs.json.JsArray
import play.api.mvc.*
import views.*

import lila.app.{ given, * }
import lila.common.{ HTTPRequest, LpvEmbed }
import lila.game.{ PgnDump, Pov }
import lila.round.JsonView.WithFlags
import lila.oauth.AccessToken

final class Analyse(
    env: Env,
    gameC: => Game,
    roundC: => Round
) extends LilaController(env):

  def requestAnalysis(id: GameId) = Auth { ctx ?=> me ?=>
    Found(env.game.gameRepo game id): game =>
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
        .map: result =>
          result.error match
            case None        => NoContent
            case Some(error) => BadRequest(error)
  }

  def replay(pov: Pov, userTv: Option[lila.user.User])(using ctx: Context) =
    if HTTPRequest.isCrawler(ctx.req).yes then replayBot(pov)
    else
      for
        initialFen <- env.game.gameRepo initialFen pov.gameId
        users      <- env.user.api.gamePlayers(pov.game.players.map(_.userId), pov.game.perfType)
        _ = gameC.preloadUsers(users)
        res <- RedirectAtFen(pov, initialFen):
          (
            env.analyse.analyser.get(pov.game),
            !pov.game.metadata.analysed so env.fishnet.api.userAnalysisExists(pov.gameId),
            pov.game.simulId so env.simul.repo.find,
            roundC.getWatcherChat(pov.game),
            ctx.noBlind so env.game.crosstableApi.withMatchup(pov.game),
            env.bookmark.api.exists(pov.game, ctx.me),
            env.api.pgnDump(
              pov.game,
              initialFen,
              analysis = none,
              PgnDump.WithFlags(clocks = false, rating = ctx.pref.showRatings)
            )
          ).flatMapN: (analysis, analysisInProgress, simul, chat, crosstable, bookmarked, pgn) =>
            env.api.roundApi
              .review(
                pov,
                users,
                tv = userTv.map: u =>
                  lila.round.OnTv.User(u.id),
                analysis,
                initialFen = initialFen,
                withFlags = WithFlags(
                  movetimes = true,
                  clocks = true,
                  division = true,
                  opening = true,
                  rating = ctx.pref.showRatings,
                  lichobileCompat = HTTPRequest.isLichobile(ctx.req),
                  puzzles = true
                )
              )
              .flatMap: data =>
                Ok.page(
                  html.analyse.replay(
                    pov,
                    data,
                    initialFen,
                    env.analyse.annotator(pgn, pov.game, analysis).render,
                    analysis,
                    analysisInProgress,
                    simul,
                    crosstable,
                    userTv,
                    chat,
                    bookmarked = bookmarked
                  )
                ).map(_.enableSharedArrayBuffer)
      yield res

  def embed(gameId: GameId, color: String) = embedReplayGame(gameId, color)

  val AcceptsPgn = Accepting("application/x-chess-pgn")

  def embedReplayGame(gameId: GameId, color: String) = Anon:
    InEmbedContext:
      env.api.textLpvExpand.getPgn(gameId) map {
        case Some(LpvEmbed.PublicPgn(pgn)) =>
          render:
            case AcceptsPgn() => Ok(pgn)
            case _            => Ok(html.analyse.embed.lpv(pgn, chess.Color.fromName(color), getPgn = true))
        case _ =>
          render:
            case AcceptsPgn() => NotFound("*")
            case _            => NotFound(html.analyse.embed.notFound)
      }

  private def RedirectAtFen(pov: Pov, initialFen: Option[Fen.Epd])(or: => Fu[Result])(using
      Context
  ): Fu[Result] =
    (get("fen").map(Fen.Epd.clean): Option[Fen.Epd]).fold(or): atFen =>
      val url = routes.Round.watcher(pov.gameId, pov.color.name)
      chess.Replay
        .plyAtFen(pov.game.sans, initialFen, pov.game.variant, atFen)
        .fold(
          err =>
            lila.log("analyse").info(s"RedirectAtFen: ${pov.gameId} $atFen $err")
            Redirect(url)
          ,
          ply => Redirect(s"$url#$ply")
        )

  private def replayBot(pov: Pov)(using Context) = for
    initialFen <- env.game.gameRepo initialFen pov.gameId
    analysis   <- env.analyse.analyser get pov.game
    simul      <- pov.game.simulId so env.simul.repo.find
    crosstable <- env.game.crosstableApi.withMatchup(pov.game)
    pgn        <- env.api.pgnDump(pov.game, initialFen, analysis, PgnDump.WithFlags(clocks = false))
    page <- renderPage:
      html.analyse.replayBot(
        pov,
        initialFen,
        env.analyse.annotator(pgn, pov.game, analysis).toString,
        simul,
        crosstable
      )
  yield Ok(page)

  def externalEngineList = ScopedBody(_.Engine.Read) { _ ?=> me ?=>
    env.analyse.externalEngine.list(me) map { list =>
      JsonOk(JsArray(list map lila.analyse.ExternalEngine.jsonWrites.writes))
    }
  }

  def externalEngineShow(id: String) = ScopedBody(_.Engine.Read) { _ ?=> me ?=>
    Found(env.analyse.externalEngine.find(me, id)): engine =>
      JsonOk(lila.analyse.ExternalEngine.jsonWrites.writes(engine))
  }

  def externalEngineCreate = ScopedBody(_.Engine.Write) { ctx ?=> me ?=>
    HTTPRequest.bearer(ctx.req) so { bearer =>
      val tokenId = AccessToken.Id from bearer
      lila.analyse.ExternalEngine.form
        .bindFromRequest()
        .fold(
          jsonFormError,
          data =>
            env.analyse.externalEngine.create(me, data, tokenId.value) map { engine =>
              Created(lila.analyse.ExternalEngine.jsonWrites.writes(engine))
            }
        )
    }
  }

  def externalEngineUpdate(id: String) = ScopedBody(_.Engine.Write) { ctx ?=> me ?=>
    Found(env.analyse.externalEngine.find(me, id)): engine =>
      lila.analyse.ExternalEngine.form
        .bindFromRequest()
        .fold(
          jsonFormError,
          data =>
            env.analyse.externalEngine.update(engine, data) map { engine =>
              JsonOk(lila.analyse.ExternalEngine.jsonWrites.writes(engine))
            }
        )
  }

  def externalEngineDelete(id: String) = ScopedBody(_.Engine.Write) { _ ?=> me ?=>
    env.analyse.externalEngine.delete(me, id) elseNotFound jsonOkResult
  }

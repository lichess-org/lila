package controllers

import chess.format.Fen
import play.api.libs.json.JsArray
import play.api.mvc.*
import views.*

import lila.api.Context
import lila.app.{ given, * }
import lila.common.HTTPRequest
import lila.game.{ PgnDump, Pov }
import lila.round.JsonView.WithFlags
import lila.oauth.AccessToken

final class Analyse(
    env: Env,
    gameC: => Game,
    roundC: => Round
) extends LilaController(env):

  def requestAnalysis(id: GameId) = Auth { ctx ?=> me =>
    OptionFuResult(env.game.gameRepo game id) { game =>
      env.fishnet.analyser(
        game,
        lila.fishnet.Work.Sender(
          userId = me.id,
          ip = ctx.ip.some,
          mod = isGranted(_.UserEvaluate) || isGranted(_.Relay),
          system = false
        )
      ) map { result =>
        result.error match
          case None        => NoContent
          case Some(error) => BadRequest(error)
      }
    }
  }

  def replay(pov: Pov, userTv: Option[lila.user.User])(using ctx: Context) =
    if (HTTPRequest.isCrawler(ctx.req).yes) replayBot(pov)
    else
      env.game.gameRepo initialFen pov.gameId flatMap { initialFen =>
        gameC.preloadUsers(pov.game) >> RedirectAtFen(pov, initialFen) {
          (env.analyse.analyser get pov.game) zip
            (!pov.game.metadata.analysed ?? env.fishnet.api.userAnalysisExists(pov.gameId)) zip
            (pov.game.simulId ?? env.simul.repo.find) zip
            roundC.getWatcherChat(pov.game) zip
            (ctx.noBlind ?? env.game.crosstableApi.withMatchup(pov.game)) zip
            env.bookmark.api.exists(pov.game, ctx.me) zip
            env.api.pgnDump(
              pov.game,
              initialFen,
              analysis = none,
              PgnDump.WithFlags(clocks = false, rating = ctx.pref.showRatings)
            ) flatMap {
              case ((((((analysis, analysisInProgress), simul), chat), crosstable), bookmarked), pgn) =>
                env.api.roundApi.review(
                  pov,
                  lila.api.Mobile.Api.currentVersion,
                  tv = userTv.map { u =>
                    lila.round.OnTv.User(u.id)
                  },
                  analysis,
                  initialFen = initialFen,
                  withFlags = WithFlags(
                    movetimes = true,
                    clocks = true,
                    division = true,
                    opening = true,
                    rating = ctx.pref.showRatings,
                    puzzles = true
                  )
                ) map { data =>
                  Ok(
                    html.analyse.replay(
                      pov,
                      data,
                      initialFen,
                      env.analyse.annotator(pgn, pov.game, analysis).toString,
                      analysis,
                      analysisInProgress,
                      simul,
                      crosstable,
                      userTv,
                      chat,
                      bookmarked = bookmarked
                    )
                  ).enableSharedArrayBuffer
                }
            }
        }
      }

  def embed(gameId: GameId, color: String) = embedReplayGame(gameId, color)

  val AcceptsPgn = Accepting("application/x-chess-pgn")

  def embedReplayGame(gameId: GameId, color: String) = Anon:
    env.api.textLpvExpand.getPgn(gameId) map {
      case Some(pgn) =>
        render {
          case AcceptsPgn() => Ok(pgn)
          case _            => Ok(html.analyse.embed.lpv(pgn, chess.Color.fromName(color)))
        }.enableSharedArrayBuffer
      case _ =>
        render {
          case AcceptsPgn() => NotFound("*")
          case _            => NotFound(html.analyse.embed.notFound)
        }
    }

  private def RedirectAtFen(pov: Pov, initialFen: Option[Fen.Epd])(or: => Fu[Result])(using Context) =
    (get("fen").map(Fen.Epd.clean): Option[Fen.Epd]).fold(or) { atFen =>
      val url = routes.Round.watcher(pov.gameId, pov.color.name)
      fuccess {
        chess.Replay
          .plyAtFen(pov.game.sans, initialFen, pov.game.variant, atFen)
          .fold(
            err => {
              lila.log("analyse").info(s"RedirectAtFen: ${pov.gameId} $atFen $err")
              Redirect(url)
            },
            ply => Redirect(s"$url#$ply")
          )
      }
    }

  private def replayBot(pov: Pov)(using Context) =
    for
      initialFen <- env.game.gameRepo initialFen pov.gameId
      analysis   <- env.analyse.analyser get pov.game
      simul      <- pov.game.simulId ?? env.simul.repo.find
      crosstable <- env.game.crosstableApi.withMatchup(pov.game)
      pgn        <- env.api.pgnDump(pov.game, initialFen, analysis, PgnDump.WithFlags(clocks = false))
    yield Ok(
      html.analyse.replayBot(
        pov,
        initialFen,
        env.analyse.annotator(pgn, pov.game, analysis).toString,
        simul,
        crosstable
      )
    )

  def externalEngineList = ScopedBody(_.Engine.Read) { _ ?=> me =>
    env.analyse.externalEngine.list(me) map { list =>
      JsonOk(JsArray(list map lila.analyse.ExternalEngine.jsonWrites.writes))
    }
  }

  def externalEngineShow(id: String) = ScopedBody(_.Engine.Read) { _ ?=> me =>
    env.analyse.externalEngine.find(me, id) map {
      _.fold(notFoundJsonSync()) { engine =>
        JsonOk(lila.analyse.ExternalEngine.jsonWrites.writes(engine))
      }
    }
  }

  def externalEngineCreate = ScopedBody(_.Engine.Write) { req ?=> me =>
    HTTPRequest.bearer(req) ?? { bearer =>
      val tokenId = AccessToken.Id from bearer
      lila.analyse.ExternalEngine.form
        .bindFromRequest()
        .fold(
          err => newJsonFormError(err)(using me.realLang | reqLang),
          data =>
            env.analyse.externalEngine.create(me, data, tokenId.value) map { engine =>
              Created(lila.analyse.ExternalEngine.jsonWrites.writes(engine))
            }
        )
    }
  }

  def externalEngineUpdate(id: String) = ScopedBody(_.Engine.Write) { req ?=> me =>
    env.analyse.externalEngine.find(me, id) flatMap {
      _.fold(notFoundJson()) { engine =>
        lila.analyse.ExternalEngine.form
          .bindFromRequest()
          .fold(
            err => newJsonFormError(err)(using me.realLang | reqLang),
            data =>
              env.analyse.externalEngine.update(engine, data) map { engine =>
                JsonOk(lila.analyse.ExternalEngine.jsonWrites.writes(engine))
              }
          )
      }
    }
  }

  def externalEngineDelete(id: String) = ScopedBody(_.Engine.Write) { _ ?=> me =>
    env.analyse.externalEngine.delete(me, id) map {
      if _ then jsonOkResult else notFoundJsonSync()
    }
  }

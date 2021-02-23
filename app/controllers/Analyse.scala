package controllers

import chess.format.FEN
import chess.White
import play.api.mvc._
import scala.concurrent.duration._
import views._

import lila.api.Context
import lila.app._
import lila.common.HTTPRequest
import lila.game.{ PgnDump, Pov }
import lila.round.JsonView.WithFlags

final class Analyse(
    env: Env,
    gameC: => Game,
    roundC: => Round
) extends LilaController(env) {

  def requestAnalysis(id: String) =
    Auth { implicit ctx => me =>
      OptionFuResult(env.game.gameRepo game id) { game =>
        env.fishnet.analyser(
          game,
          lila.fishnet.Work.Sender(
            userId = me.id,
            ip = HTTPRequest.ipAddress(ctx.req).some,
            mod = isGranted(_.Hunter) || isGranted(_.Relay),
            system = false
          )
        ) map {
          case true  => NoContent
          case false => Unauthorized
        }
      }
    }

  def multipleAnalysis(username: String) =
    SecureBody(_.Hunter) { implicit ctx => me =>
      OptionFuResult(env.user.repo named username) { user =>
        implicit val body = ctx.body
        import play.api.data._
        import play.api.data.Forms._
        Form(single("game" -> list(nonEmptyText)))
          .bindFromRequest()
          .fold(
            err => BadRequest(err.toString).fuccess,
            gameIds =>
              env.game.gameRepo.unanalysedGames(gameIds).flatMap { games =>
                games.map { game =>
                  env.fishnet.analyser(
                    game,
                    lila.fishnet.Work.Sender(
                      userId = me.id,
                      ip = HTTPRequest.ipAddress(ctx.req).some,
                      mod = true,
                      system = false
                    )
                  )
                }.sequenceFu >>
                  env.fishnet.awaiter(games.map(_.id), 2 minutes) >>
                  env.mod.assessApi.ensurePlayerAssess(user.id, games)
              } inject NoContent
          )
      }
    }

  def replay(pov: Pov, userTv: Option[lila.user.User])(implicit ctx: Context) =
    if (HTTPRequest isCrawler ctx.req) replayBot(pov)
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
              PgnDump.WithFlags(clocks = false)
            ) flatMap { case analysis ~ analysisInProgress ~ simul ~ chat ~ crosstable ~ bookmarked ~ pgn =>
              env.api.roundApi.review(
                pov,
                lila.api.Mobile.Api.currentVersion,
                tv = userTv.map { u =>
                  lila.round.OnUserTv(u.id)
                },
                analysis,
                initialFenO = initialFen.some,
                withFlags = WithFlags(
                  movetimes = true,
                  clocks = true,
                  division = true,
                  opening = true
                )
              ) map { data =>
                EnableSharedArrayBuffer(
                  Ok(
                    html.analyse.replay(
                      pov,
                      data,
                      initialFen,
                      env.analyse
                        .annotator(pgn, analysis, pov.game.opening, pov.game.winnerColor, pov.game.status)
                        .toString,
                      analysis,
                      analysisInProgress,
                      simul,
                      crosstable,
                      userTv,
                      chat,
                      bookmarked = bookmarked
                    )
                  )
                )
              }
            }
        }
      }

  def embed(gameId: String, color: String) =
    Action.async { implicit req =>
      env.game.gameRepo.gameWithInitialFen(gameId) flatMap {
        case Some((game, initialFen)) =>
          val pov = Pov(game, chess.Color.fromName(color) | White)
          env.api.roundApi.embed(
            pov,
            lila.api.Mobile.Api.currentVersion,
            initialFenO = initialFen.some,
            withFlags = WithFlags(opening = true)
          ) map { data =>
            Ok(html.analyse.embed(pov, data))
          }
        case _ => fuccess(NotFound(html.analyse.embed.notFound))
      } dmap EnableSharedArrayBuffer
    }

  private def RedirectAtFen(pov: Pov, initialFen: Option[FEN])(or: => Fu[Result])(implicit ctx: Context) =
    get("fen").map(FEN.clean).fold(or) { atFen =>
      val url = routes.Round.watcher(pov.gameId, pov.color.name)
      fuccess {
        chess.Replay
          .plyAtFen(pov.game.pgnMoves, initialFen, pov.game.variant, atFen)
          .fold(
            err => {
              lila.log("analyse").info(s"RedirectAtFen: ${pov.gameId} $atFen $err")
              Redirect(url)
            },
            ply => Redirect(s"$url#$ply")
          )
      }
    }

  private def replayBot(pov: Pov)(implicit ctx: Context) =
    for {
      initialFen <- env.game.gameRepo initialFen pov.gameId
      analysis   <- env.analyse.analyser get pov.game
      simul      <- pov.game.simulId ?? env.simul.repo.find
      crosstable <- env.game.crosstableApi.withMatchup(pov.game)
      pgn        <- env.api.pgnDump(pov.game, initialFen, analysis, PgnDump.WithFlags(clocks = false))
    } yield Ok(
      html.analyse.replayBot(
        pov,
        initialFen,
        env.analyse
          .annotator(pgn, analysis, pov.game.opening, pov.game.winnerColor, pov.game.status)
          .toString,
        simul,
        crosstable
      )
    )
}

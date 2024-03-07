package controllers

import play.api.mvc._

import shogi.format.forsyth.Sfen
import lila.api.Context
import lila.app._
import lila.common.HTTPRequest
import lila.game.{ NotationDump, Pov }
import lila.round.JsonView.WithFlags
import views._

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
            userId = me.id.some,
            postGameStudy = none,
            ip = HTTPRequest.lastRemoteAddress(ctx.req).some,
            mod = isGranted(_.Hunter),
            system = false
          )
        ) map {
          case true  => NoContent
          case false => Unauthorized
        }
      }
    }

  def replay(pov: Pov, userTv: Option[lila.user.User])(implicit ctx: Context) =
    if (HTTPRequest isCrawler ctx.req) replayBot(pov)
    else
      gameC.preloadUsers(pov.game) >> RedirectAtSfen(pov) {
        (env.analyse.analyser get pov.game) zip
          (!pov.game.metadata.analysed ?? env.fishnet.api.userAnalysisExists(pov.gameId)) zip
          (pov.game.simulId ?? env.simul.repo.find) zip
          roundC.getWatcherChat(pov.game) zip
          (ctx.noBlind ?? env.game.crosstableApi.withMatchup(pov.game)) zip
          env.bookmark.api.exists(pov.game, ctx.me) zip
          env.api.notationDump(
            pov.game,
            analysis = none,
            NotationDump.WithFlags(clocks = false)
          ) flatMap {
            case ((((((analysis, analysisInProgress), simul), chat), crosstable), bookmarked), kif) =>
              env.api.roundApi.review(
                pov,
                lila.api.Mobile.Api.currentVersion,
                tv = userTv.map { u =>
                  lila.round.OnUserTv(u.id)
                },
                analysis,
                withFlags = WithFlags(
                  movetimes = true,
                  clocks = true,
                  division = true
                )
              ) map { data =>
                Ok(
                  html.analyse.replay(
                    pov,
                    data,
                    kif.render,
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

  def embed(gameId: String, color: String) =
    Action.async { implicit req =>
      env.game.gameRepo.game(gameId) flatMap {
        case Some(game) =>
          val pov = Pov(game, shogi.Color.fromSente(color == "sente"))
          env.api.roundApi.embed(
            pov,
            lila.api.Mobile.Api.currentVersion,
            withFlags = WithFlags()
          ) map { data =>
            Ok(html.analyse.embed(pov, data)).enableSharedArrayBuffer
          }
        case _ => fuccess(NotFound(html.analyse.embed.notFound))
      }
    }

  private def RedirectAtSfen(pov: Pov)(or: => Fu[Result])(implicit ctx: Context) =
    get("sfen").map(Sfen.clean).fold(or) { atSfen =>
      val url = routes.Round.watcher(pov.gameId, pov.color.name)
      fuccess {
        shogi.Replay
          .plyAtSfen(pov.game.usis, pov.game.initialSfen, pov.game.variant, atSfen)
          .fold(
            err => {
              lila.log("analyse").info(s"RedirectAtSfen: ${pov.gameId} $atSfen $err")
              Redirect(url)
            },
            ply => Redirect(s"$url#$ply")
          )
      }
    }

  private def replayBot(pov: Pov)(implicit ctx: Context) =
    for {
      analysis   <- env.analyse.analyser get pov.game
      simul      <- pov.game.simulId ?? env.simul.repo.find
      crosstable <- env.game.crosstableApi.withMatchup(pov.game)
      kif        <- env.api.notationDump(pov.game, analysis, NotationDump.WithFlags(clocks = false))
    } yield Ok(
      html.analyse.replayBot(
        pov,
        kif.render,
        simul,
        crosstable
      )
    )
}

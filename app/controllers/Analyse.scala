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
            ip = HTTPRequest.lastRemoteAddress(ctx.req).some,
            mod = isGranted(_.Hunter) || isGranted(_.Relay),
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
      env.game.gameRepo initialSfen pov.gameId flatMap { initialSfen =>
        gameC.preloadUsers(pov.game) >> RedirectAtSfen(pov, initialSfen) {
          (env.analyse.analyser get pov.game) zip
            (!pov.game.metadata.analysed ?? env.fishnet.api.userAnalysisExists(pov.gameId)) zip
            (pov.game.simulId ?? env.simul.repo.find) zip
            roundC.getWatcherChat(pov.game) zip
            (ctx.noBlind ?? env.game.crosstableApi.withMatchup(pov.game)) zip
            env.bookmark.api.exists(pov.game, ctx.me) zip
            env.api.notationDump(
              pov.game,
              initialSfen,
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
                  initialSfenO = initialSfen.some,
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
                        initialSfen,
                        kif.render,
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
      env.game.gameRepo.gameWithInitialSfen(gameId) flatMap {
        case Some((game, initialSfen)) =>
          val pov = Pov(game, shogi.Color.fromSente(color == "sente"))
          env.api.roundApi.embed(
            pov,
            lila.api.Mobile.Api.currentVersion,
            initialSfenO = initialSfen.some,
            withFlags = WithFlags(opening = true)
          ) map { data =>
            Ok(html.analyse.embed(pov, data))
          }
        case _ => fuccess(NotFound(html.analyse.embed.notFound))
      } dmap EnableSharedArrayBuffer
    }

  private def RedirectAtSfen(pov: Pov, initialSfen: Option[Sfen])(or: => Fu[Result])(implicit ctx: Context) =
    get("sfen").map(Sfen.clean).fold(or) { atSfen =>
      val url = routes.Round.watcher(pov.gameId, pov.color.name)
      fuccess {
        shogi.Replay
          .plyAtSfen(pov.game.usiMoves, initialSfen, pov.game.variant, atSfen)
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
      initialSfen <- env.game.gameRepo initialSfen pov.gameId
      analysis   <- env.analyse.analyser get pov.game
      simul      <- pov.game.simulId ?? env.simul.repo.find
      crosstable <- env.game.crosstableApi.withMatchup(pov.game)
      kif        <- env.api.notationDump(pov.game, initialSfen, analysis, NotationDump.WithFlags(clocks = false))
    } yield Ok(
      html.analyse.replayBot(
        pov,
        initialSfen,
        kif.render,
        simul,
        crosstable
      )
    )
}

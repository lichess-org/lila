package controllers

import lila.app._
import play.api.libs.json.Json
import views._

object Importer extends LilaController {

  private def env = Env.importer

  def importGame = Open { implicit ctx =>
    fuccess {
      Ok(html.game.importGame(env.forms.importForm))
    }
  }

  def sendGame = OpenBody { implicit ctx =>
    implicit def req = ctx.body
    env.forms.importForm.bindFromRequest.fold(
      failure => fuccess {
        Ok(html.game.importGame(failure))
      },
      data => env.importer(data, ctx.userId) map { game =>
        if (data.analyse.isDefined && game.analysable) Analyse.addCallbacks(game.id) {
          Env.analyse.analyser.getOrGenerate(
            game.id,
            ctx.userId | "lichess",
            userIp = ctx.req.remoteAddress.some,
            concurrent = false,
            auto = false)
        }
        Redirect(routes.Round.watcher(game.id, "white"))
      } recover {
        case e => {
          logwarn(e.getMessage)
          Redirect(routes.Importer.importGame)
        }
      }
    )
  }

  import lila.game.GameRepo
  import org.joda.time.DateTime
  private val masterGameEncodingFixedAt = new DateTime(2016, 3, 9, 0, 0)

  def masterGame(id: String, orientation: String) = Open { implicit ctx =>
    def redirectAtFen(game: lila.game.Game) = Redirect {
      val url = routes.Round.watcher(game.id, orientation).url
      val fenParam = get("fen").??(f => s"?fen=$f")
      s"$url$fenParam"
    }
    GameRepo game id flatMap {
      case Some(game) if game.createdAt.isAfter(masterGameEncodingFixedAt) => fuccess(redirectAtFen(game))
      case _ => (GameRepo remove id) >> Env.explorer.fetchPgn(id) flatMap {
        case None => fuccess(NotFound)
        case Some(pgn) => env.importer(
          lila.importer.ImportData(pgn, none),
          user = "lichess".some,
          forceId = id.some) map redirectAtFen
      }
    }
  }
}

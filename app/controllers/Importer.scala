package controllers

import lila.app._
import play.api.libs.json.Json
import views._

object Importer extends LilaController with BaseGame {

  private def env = Env.importer

  def importGame = Open { implicit ctx =>
    makeListMenu map { listMenu =>
      Ok(html.game.importGame(listMenu, env.forms.importForm))
    }
  }

  def sendGame = OpenBody { implicit ctx =>
    implicit def req = ctx.body
    env.forms.importForm.bindFromRequest.fold(
      failure => makeListMenu map { listMenu =>
        Ok(html.game.importGame(listMenu, failure))
      },
      data => env.importer(data, ctx.userId, ctx.ip) map { game =>
        Analyse.addCallbacks(game.id) {
          Env.analyse.analyser.getOrGenerate(game.id, ctx.userId | "lichess", concurrent = true, auto = false)
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
}

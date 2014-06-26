package controllers

import lila.app._
import play.api.libs.json.Json
import views._

object Importer extends LilaController with BaseGame {

  private def env = Env.importer

  def liveCreate = Open { implicit ctx =>
    env.live.create map { g =>
      Ok(Json.obj(
        "id" -> g.id,
        "url" -> s"${Env.api.Net.BaseUrl}${routes.Round.watcher(g.id, "white").url}"
      )) as JSON
    }
  }

  def liveMove(id: String, move: String) = Open { implicit ctx =>
    env.live.move(id, move, ctx.ip) inject
      (Ok(Json.obj()) as JSON) recover {
        case e: Exception => BadRequest(Json.obj("error" -> e.getMessage)) as JSON
      }
  }

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

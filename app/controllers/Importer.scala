package controllers

import lidraughts.app._
import lidraughts.common.HTTPRequest
import play.api.libs.json.Json
import views._

object Importer extends LidraughtsController {

  private def env = Env.importer

  def importGame = OpenBody { implicit ctx =>
    fuccess {
      val pdn = ctx.body.queryString.get("pdn").flatMap(_.headOption).getOrElse("")
      val data = lidraughts.importer.ImportData(pdn, None)
      Ok(html.game.importGame(env.forms.importForm.fill(data)))
    }
  }

  def sendGame = OpenBody { implicit ctx =>
    implicit def req = ctx.body
    env.forms.importForm.bindFromRequest.fold(
      failure => negotiate(
        html = Ok(html.game.importGame(failure)).fuccess,
        api = _ => BadRequest(Json.obj("error" -> "Invalid PDN")).fuccess
      ),
      data => env.importer(data, ctx.userId) flatMap { game =>
        (ctx.userId ?? Env.game.cached.clearNbImportedByCache) >>
          (data.analyse.isDefined && game.analysable) ?? {
            Env.draughtsnet.analyser(game, lidraughts.draughtsnet.Work.Sender(
              userId = ctx.userId,
              ip = HTTPRequest.lastRemoteAddress(ctx.req).some,
              mod = isGranted(_.Hunter),
              system = false
            ))
          } inject Redirect(routes.Round.watcher(game.id, "white"))
      } recover {
        case e =>
          controllerLogger.branch("importer").warn(
            s"Imported game validates but can't be replayed:\n${data.pdn}", e
          )
          Redirect(routes.Importer.importGame)
      }
    )
  }

  def masterGame(id: String, orientation: String) = Open { implicit ctx =>
    Env.explorer.importer(id) map {
      _ ?? { game =>
        val url = routes.Round.watcher(game.id, orientation).url
        val fenParam = get("fen").??(f => s"?fen=$f")
        Redirect(s"$url$fenParam")
      }
    }
  }
}

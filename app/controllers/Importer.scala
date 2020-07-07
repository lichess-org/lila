package controllers

import lila.app._
import lila.common.HTTPRequest
import play.api.libs.json.Json
import views._

final class Importer(env: Env) extends LilaController(env) {

  def importGame =
    OpenBody { implicit ctx =>
      fuccess {
        val pgn  = ctx.body.queryString.get("pgn").flatMap(_.headOption).getOrElse("")
        val data = lila.importer.ImportData(pgn, None)
        Ok(html.game.importGame(env.importer.forms.importForm.fill(data)))
      }
    }

  def sendGame =
    OpenBody { implicit ctx =>
      implicit def req = ctx.body
      env.importer.forms.importForm.bindFromRequest().fold(
        failure =>
          negotiate(
            html = Ok(html.game.importGame(failure)).fuccess,
            api = _ => BadRequest(Json.obj("error" -> "Invalid PGN")).fuccess
          ),
        data =>
          env.importer.importer(data, ctx.userId) flatMap { game =>
            (ctx.userId ?? env.game.cached.clearNbImportedByCache) >>
              (data.analyse.isDefined && game.analysable) ?? {
                env.fishnet.analyser(
                  game,
                  lila.fishnet.Work.Sender(
                    userId = ctx.userId,
                    ip = HTTPRequest.lastRemoteAddress(ctx.req).some,
                    mod = isGranted(_.Hunter) || isGranted(_.Relay),
                    system = false
                  )
                )
              } inject Redirect(routes.Round.watcher(game.id, "white"))
          } recover {
            case e =>
              lila
                .log("importer")
                .warn(
                  s"Imported game validates but can't be replayed:\n${data.pgn}",
                  e
                )
              Redirect(routes.Importer.importGame())
          }
      )
    }

  def masterGame(id: String, orientation: String) =
    Open { implicit ctx =>
      env.explorer.importer(id) map {
        _ ?? { game =>
          val url      = routes.Round.watcher(game.id, orientation).url
          val fenParam = get("fen").??(f => s"?fen=$f")
          Redirect(s"$url$fenParam")
        }
      }
    }
}

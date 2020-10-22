package controllers

import scala.concurrent.duration._
import play.api.mvc._

import lila.app._
import lila.common.{ HTTPRequest, IpAddress }
import play.api.libs.json.Json
import views._

final class Importer(env: Env) extends LilaController(env) {

  private val ImportRateLimitPerIP = new lila.memo.RateLimit[IpAddress](
    credits = 200,
    duration = 1.hour,
    key = "import.game.ip"
  )

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
      env.importer.forms.importForm
        .bindFromRequest()
        .fold(
          failure =>
            negotiate( // used by mobile app
              html = Ok(html.game.importGame(failure)).fuccess,
              api = _ => BadRequest(jsonError("Invalid PGN")).fuccess
            ),
          data =>
            ImportRateLimitPerIP(HTTPRequest ipAddress req, cost = 1) {
              doImport(data, req, ctx.me) flatMap {
                case Some(game) =>
                  (data.analyse.isDefined && game.analysable) ?? {
                    env.fishnet.analyser(
                      game,
                      lila.fishnet.Work.Sender(
                        userId = ctx.userId,
                        ip = HTTPRequest.ipAddress(ctx.req).some,
                        mod = isGranted(_.Hunter) || isGranted(_.Relay),
                        system = false
                      )
                    )
                  } inject Redirect(routes.Round.watcher(game.id, "white"))
                case None => Redirect(routes.Importer.importGame()).fuccess
              }
            }(rateLimitedFu)
        )
    }

  def apiSendGame = {
    def commonImport(req: Request[_], me: Option[lila.user.User]): Fu[Result] =
      ImportRateLimitPerIP(HTTPRequest ipAddress req, cost = if (me.isDefined) 1 else 2) {
        env.importer.forms.importForm
          .bindFromRequest()(req)
          .fold(
            err => BadRequest(apiFormError(err)).fuccess,
            data =>
              doImport(data, req, me) map {
                _.fold(BadRequest(jsonError("The PGN could not be replayed"))) { game =>
                  JsonOk {
                    Json.obj(
                      "id"  -> game.id,
                      "url" -> s"${env.net.baseUrl}/${game.id}"
                    )
                  }
                }
              }
          )
      }(rateLimitedFu)
    AnonOrScopedBody(parse.anyContent)()(
      anon = req => commonImport(req, none),
      scoped = req => me => commonImport(req, me.some)
    )
  }

  private def doImport(
      data: lila.importer.ImportData,
      req: RequestHeader,
      me: Option[lila.user.User]
  ): Fu[Option[lila.game.Game]] =
    env.importer.importer(data, me.map(_.id)) flatMap { game =>
      me.map(_.id).??(env.game.cached.clearNbImportedByCache) inject game.some
    } recover { case e: Exception =>
      lila
        .log("importer")
        .warn(s"Imported game validates but can't be replayed:\n${data.pgn}", e)
      none
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

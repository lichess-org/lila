package controllers

import play.api.libs.json.Json
import scala.util.{ Either, Left, Right }
import play.api.mvc.*
import views.*

import lila.app.{ given, * }
import lila.common.IpAddress
import chess.format.pgn.PgnStr

final class Importer(env: Env) extends LilaController(env):

  private val ImportRateLimitPerIP = lila.memo.RateLimit.composite[IpAddress](
    key = "import.game.ip",
    enforce = env.net.rateLimit.value
  )(
    ("fast", 10, 1.minute),
    ("slow", 150, 1.hour)
  )

  def importGame = OpenBody:
    fuccess:
      val pgn  = reqBody.queryString.get("pgn").flatMap(_.headOption).getOrElse("")
      val data = lila.importer.ImportData(PgnStr(pgn), None)
      Ok(html.game.importGame(env.importer.forms.importForm.fill(data)))

  def sendGame = OpenBody:
    env.importer.forms.importForm
      .bindFromRequest()
      .fold(
        failure =>
          negotiate( // used by mobile app
            html = Ok(html.game.importGame(failure)).toFuccess,
            api = _ => BadRequest(jsonError("Invalid PGN")).toFuccess
          ),
        data =>
          ImportRateLimitPerIP(ctx.ip, rateLimitedFu, cost = 1):
            doImport(data, ctx.me) flatMap {
              case Right(game) =>
                ctx.me.filter(_ => data.analyse.isDefined && game.analysable) ?? { me =>
                  env.fishnet
                    .analyser(
                      game,
                      lila.fishnet.Work.Sender(
                        userId = me.id,
                        ip = ctx.ip.some,
                        mod = isGranted(_.UserEvaluate) || isGranted(_.Relay),
                        system = false
                      )
                    )
                    .void
                } inject Redirect(routes.Round.watcher(game.id, "white"))
              case Left(error) => Redirect(routes.Importer.importGame).flashFailure(error).toFuccess
            }
      )

  def apiSendGame =
    AnonOrScopedBody(parse.anyContent)() { req ?=> me =>
      ImportRateLimitPerIP(req.ipAddress, rateLimitedFu, cost = if me.isDefined then 1 else 2):
        env.importer.forms.importForm
          .bindFromRequest()(req, formBinding)
          .fold(
            err => BadRequest(apiFormError(err)).toFuccess,
            data =>
              doImport(data, me).map:
                case Left(error) => BadRequest(jsonError(error))
                case Right(game) =>
                  JsonOk:
                    Json.obj(
                      "id"  -> game.id,
                      "url" -> s"${env.net.baseUrl}/${game.id}"
                    )
          )
    }

  private def doImport(
      data: lila.importer.ImportData,
      me: Option[lila.user.User]
  ): Fu[Either[String, lila.game.Game]] =
    env.importer.importer(data, me.map(_.id)) flatMap { game =>
      me.map(_.id).??(env.game.cached.clearNbImportedByCache) inject Right(game)
    } recover { case _: Exception =>
      Left("The PGN contains illegal and/or ambiguous moves.")
    }

  def masterGame(id: GameId, orientation: String) = Open:
    env.explorer.importer(id) mapz { game =>
      val url      = routes.Round.watcher(game.id, orientation).url
      val fenParam = get("fen").??(f => s"?fen=$f")
      Redirect(s"$url$fenParam")
    }

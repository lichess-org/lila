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
    val pgn  = reqBody.queryString.get("pgn").flatMap(_.headOption).getOrElse("")
    val data = lila.importer.ImportData(PgnStr(pgn), None)
    Ok.page(html.game.importGame(env.importer.forms.importForm.fill(data)))

  def sendGame = OpenOrScopedBody(parse.anyContent)(): ctx ?=>
    env.importer.forms.importForm
      .bindFromRequest()
      .fold(
        err =>
          negotiate(
            BadRequest.page(html.game.importGame(err)),
            jsonFormError(err)
          ),
        data =>
          ImportRateLimitPerIP(ctx.ip, rateLimitedFu, cost = 1):
            env.importer.importer(data) flatMap { game =>
              ctx.me.so(env.game.cached.clearNbImportedByCache(_)) inject Right(game)
            } recover { case _: Exception =>
              Left("The PGN contains illegal and/or ambiguous moves.")
            } flatMap {
              case Right(game) =>
                negotiate(
                  html = ctx.me.filter(_ => data.analyse.isDefined && game.analysable) soUse { me ?=>
                    env.fishnet
                      .analyser(
                        game,
                        lila.fishnet.Work.Sender(
                          userId = me,
                          ip = ctx.ip.some,
                          mod = isGranted(_.UserEvaluate) || isGranted(_.Relay),
                          system = false
                        )
                      )
                      .void
                  } inject Redirect(routes.Round.watcher(game.id, "white")),
                  json = JsonOk:
                    Json.obj(
                      "id"  -> game.id,
                      "url" -> s"${env.net.baseUrl}/${game.id}"
                    )
                )
              case Left(error) =>
                negotiate(
                  Redirect(routes.Importer.importGame).flashFailure(error),
                  BadRequest(jsonError(error))
                )
            }
      )

  def apiSendGame = sendGame

  def masterGame(id: GameId, orientation: String) = Open:
    Found(env.explorer.importer(id)): game =>
      val url      = routes.Round.watcher(game.id, orientation).url
      val fenParam = get("fen").so(f => s"?fen=$f")
      Redirect(s"$url$fenParam")

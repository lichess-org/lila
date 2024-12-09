package controllers

import chess.format.pgn.PgnStr
import play.api.libs.json.Json
import play.api.mvc.*

import scala.util.{ Either, Left, Right }

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.common.Json.given

final class Importer(env: Env) extends LilaController(env):

  def importGame = OpenBody:
    val pgn  = reqBody.queryString.get("pgn").flatMap(_.headOption).getOrElse("")
    val data = lila.game.importer.ImportData(PgnStr(pgn), None)
    Ok.page(views.game.ui.importer(lila.game.importer.form.fill(data)))

  def sendGame    = OpenOrScopedBody(parse.anyContent)()(doSendGame)
  def apiSendGame = AnonOrScopedBody(parse.anyContent)()(doSendGame)
  private def doSendGame(using ctx: BodyContext[Any]) =
    bindForm(lila.game.importer.form)(
      err =>
        negotiate(
          BadRequest.page(views.game.ui.importer(err)),
          jsonFormError(err)
        ),
      data =>
        limit.gameImport(ctx.ip, rateLimited, cost = if ctx.isAuth then 1 else 2):
          env.game.importer
            .importAsGame(data.pgn)
            .flatMap: game =>
              ctx.me.so(env.game.cached.clearNbImportedByCache(_)).inject(Right(game))
            .recover { case _: Exception =>
              Left("The PGN contains illegal and/or ambiguous moves.")
            }
            .flatMap {
              case Right(game) =>
                negotiate(
                  html = ctx.me
                    .filter(_ => data.analyse.isDefined && lila.game.GameExt.analysable(game))
                    .soUse { me ?=>
                      env.fishnet
                        .analyser(
                          game,
                          lila.fishnet.Work.Sender(
                            userId = me,
                            ip = ctx.ip.some,
                            mod = isGranted(_.UserEvaluate),
                            system = false
                          )
                        )
                        .void
                    }
                    .inject(Redirect(routes.Round.watcher(game.id, Color.white))),
                  json =
                    if HTTPRequest.isLichobile(ctx.req)
                    then Redirect(routes.Round.watcher(game.id, Color.white))
                    else
                      JsonOk:
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

  def masterGame(id: GameId, orientation: Color) = Open:
    Found(env.explorer.importer(id)): game =>
      val url      = routes.Round.watcher(game.id, orientation).url
      val fenParam = get("fen").so(f => s"?fen=$f")
      Redirect(s"$url$fenParam")

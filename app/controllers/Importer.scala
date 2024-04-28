package controllers

import chess.ErrorStr
import chess.format.pgn.PgnStr
import play.api.libs.json.Json
import play.api.mvc.*

import scala.util.{ Either, Left, Right }

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.core.net.IpAddress
import lila.game.GameExt.analysable
import lila.core.game.ImportedGame

final class Importer(env: Env) extends LilaController(env):
  import Importer.*

  private val ImportRateLimitPerIP = lila.memo.RateLimit.composite[IpAddress](
    key = "import.game.ip",
    enforce = env.net.rateLimit.value
  )(
    ("fast", 10, 1.minute),
    ("slow", 150, 1.hour)
  )

  def importGame = OpenBody:
    val pgn  = reqBody.queryString.get("pgn").flatMap(_.headOption).getOrElse("")
    val data = ImportData(PgnStr(pgn), None)
    Ok.page(views.game.importGame(importForm.fill(data)))

  def sendGame    = OpenOrScopedBody(parse.anyContent)()(doSendGame)
  def apiSendGame = AnonOrScopedBody(parse.anyContent)()(doSendGame)
  private def doSendGame(using ctx: BodyContext[Any]) =
    importForm
      .bindFromRequest()
      .fold(
        err =>
          negotiate(
            BadRequest.page(views.game.importGame(err)),
            jsonFormError(err)
          ),
        data =>
          ImportRateLimitPerIP(ctx.ip, rateLimited, cost = if ctx.isAuth then 1 else 2):
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
                      .inject(Redirect(routes.Round.watcher(game.id, "white"))),
                    json =
                      if HTTPRequest.isLichobile(ctx.req)
                      then Redirect(routes.Round.watcher(game.id, "white"))
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

  def masterGame(id: GameId, orientation: String) = Open:
    Found(env.explorer.importer(id)): game =>
      val url      = routes.Round.watcher(game.id, orientation).url
      val fenParam = get("fen").so(f => s"?fen=$f")
      Redirect(s"$url$fenParam")

object Importer:

  import play.api.data.*
  import play.api.data.Forms.*
  import lila.common.Form.into

  val importForm = Form:
    mapping(
      "pgn"     -> nonEmptyText.into[PgnStr].verifying("invalidPgn", p => checkPgn(p).isRight),
      "analyse" -> optional(nonEmptyText)
    )(ImportData.apply)(unapply)

  private def checkPgn(pgn: PgnStr): Either[ErrorStr, ImportedGame] =
    lila.game.importer.parseImport(pgn, none)

  case class ImportData(pgn: PgnStr, analyse: Option[String])

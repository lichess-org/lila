package controllers

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.mvc.RequestHeader
import scala.concurrent.duration._

import lila.api.GameApiV2
import lila.app._
import lila.common.{ MaxPerSecond, HTTPRequest }
import lila.game.{ GameRepo, Game => GameModel }
import views._

object Game extends LilaController {

  def delete(gameId: String) = Auth { implicit ctx => me =>
    OptionFuResult(GameRepo game gameId) { game =>
      if (game.pgnImport.flatMap(_.user) ?? (me.id==)) {
        Env.hub.actor.bookmark ! lila.hub.actorApi.bookmark.Remove(game.id)
        (GameRepo remove game.id) >>
          (lila.analyse.AnalysisRepo remove game.id) >>
          Env.game.cached.clearNbImportedByCache(me.id) inject
          Redirect(routes.User.show(me.username))
      } else fuccess {
        Redirect(routes.Round.watcher(game.id, game.firstColor.name))
      }
    }
  }

  def exportOne(id: String) = Open { implicit ctx =>
    OptionFuResult(GameRepo game id) { game =>
      if (game.playable) BadRequest("Can't export PGN of game in progress").fuccess
      else {
        val config = GameApiV2.OneConfig(
          format = if (HTTPRequest acceptsJson ctx.req) GameApiV2.Format.JSON else GameApiV2.Format.PGN,
          imported = getBool("imported"),
          flags = requestPgnFlags(ctx.req)
        )
        lila.mon.export.pgn.game()
        Env.api.gameApiV2.exportOne(game, config) flatMap { content =>
          Env.api.gameApiV2.filename(game, config.format) map { filename =>
            Ok(content).withHeaders(
              CONTENT_TYPE -> gameContentType(config),
              CONTENT_DISPOSITION -> s"attachment; filename=$filename"
            )
          }
        }
      }
    }
  }

  def exportByUser(username: String) = OpenOrScoped()(
    open = ctx => handleExport(username, ctx.me, ctx.req, oauth = false),
    scoped = req => me => handleExport(username, me.some, req, oauth = true)
  )

  private def handleExport(username: String, me: Option[lila.user.User], req: RequestHeader, oauth: Boolean) =
    lila.user.UserRepo named username flatMap {
      _ ?? { user =>
        RequireHttp11(req) {
          Api.GlobalLinearLimitPerIP(HTTPRequest lastRemoteAddress req) {
            Api.GlobalLinearLimitPerUserOption(me) {
              val format = if (HTTPRequest acceptsNdJson req) GameApiV2.Format.JSON else GameApiV2.Format.PGN
              val config = GameApiV2.ByUserConfig(
                user = user,
                format = format,
                since = getLong("since", req) map { ts => new DateTime(ts) },
                until = getLong("until", req) map { ts => new DateTime(ts) },
                max = getInt("max", req) map (_ atLeast 1),
                rated = getBoolOpt("rated", req),
                perfType = ~get("perfType", req) split "," flatMap { lila.rating.PerfType(_) } toSet,
                color = get("color", req) flatMap chess.Color.apply,
                analysed = getBoolOpt("analysed", req),
                flags = requestPgnFlags(req),
                perSecond = MaxPerSecond(me match {
                  case Some(m) if m is user.id => 50
                  case Some(_) if oauth => 20 // bonus for oauth logged in only (not for XSRF)
                  case _ => 10
                })
              )
              val date = (DateTimeFormat forPattern "yyyy-MM-dd") print new DateTime
              Ok.chunked(Env.api.gameApiV2.exportByUser(config)).withHeaders(
                CONTENT_TYPE -> gameContentType(config),
                CONTENT_DISPOSITION -> s"attachment; filename=lichess_${user.username}_$date.${format.toString.toLowerCase}"
              ).fuccess
            }
          }
        }
      }
    }

  private def requestPgnFlags(req: RequestHeader) =
    lila.game.PgnDump.WithFlags(
      moves = getBoolOpt("moves", req) | true,
      tags = getBoolOpt("tags", req) | true,
      clocks = getBoolOpt("clocks", req) | false,
      evals = getBoolOpt("evals", req) | false,
      opening = getBoolOpt("opening", req) | false
    )

  private def gameContentType(config: GameApiV2.Config) = config.format match {
    case GameApiV2.Format.PGN => pgnContentType
    case GameApiV2.Format.JSON => config match {
      case _: GameApiV2.ByUserConfig => ndJsonContentType
      case _: GameApiV2.OneConfig => JSON
    }
  }

  private[controllers] def preloadUsers(game: GameModel): Funit =
    Env.user.lightUserApi preloadMany game.userIds
}

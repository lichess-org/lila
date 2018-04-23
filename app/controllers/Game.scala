package controllers

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.mvc.RequestHeader
import scala.concurrent.duration._

import lidraughts.api.PdnDump
import lidraughts.app._
import lidraughts.common.{ MaxPerSecond, HTTPRequest }
import lidraughts.game.{ GameRepo, Game => GameModel }
import views._

object Game extends LidraughtsController {

  def delete(gameId: String) = Auth { implicit ctx => me =>
    OptionFuResult(GameRepo game gameId) { game =>
      if (game.pdnImport.flatMap(_.user) ?? (me.id==)) {
        Env.hub.actor.bookmark ! lidraughts.hub.actorApi.bookmark.Remove(game.id)
        (GameRepo remove game.id) >>
          (lidraughts.analyse.AnalysisRepo remove game.id) >>
          Env.game.cached.clearNbImportedByCache(me.id) inject
          Redirect(routes.User.show(me.username))
      } else fuccess {
        Redirect(routes.Round.watcher(game.id, game.firstColor.name))
      }
    }
  }

  def export(username: String) = OpenOrScoped()(
    open = ctx => handleExport(username, ctx.me, ctx.req, ctx.pref.draughtsResult),
    scoped = req => me => handleExport(username, me.some, req, lidraughts.pref.Pref.default.draughtsResult)
  )

  private def handleExport(username: String, me: Option[lidraughts.user.User], req: RequestHeader, draughtsResult: Boolean) =
    lidraughts.user.UserRepo named username flatMap {
      _ ?? { user =>
        RequireHttp11(req) {
          Api.GlobalLinearLimitPerIP(HTTPRequest lastRemoteAddress req) {
            Api.GlobalLinearLimitPerUserOption(me) {
              val config = PdnDump.Config(
                user = user,
                since = getLong("since", req) map { ts => new DateTime(ts) },
                until = getLong("until", req) map { ts => new DateTime(ts) },
                max = getInt("max", req) map (_ atLeast 1),
                rated = getBoolOpt("rated", req),
                perfType = ~get("perfType", req) split "," flatMap { lidraughts.rating.PerfType(_) } toSet,
                color = get("color", req) flatMap draughts.Color.apply,
                flags = lidraughts.game.PdnDump.WithFlags(
                  moves = getBoolOpt("moves", req) | true,
                  tags = getBoolOpt("tags", req) | true,
                  clocks = getBoolOpt("clocks", req) | false,
                  draughtsResult = draughtsResult
                ),
                perSecond = MaxPerSecond(me match {
                  case None => 10
                  case Some(m) if m is user.id => 50
                  case Some(_) => 20
                })
              )
              val date = (DateTimeFormat forPattern "yyyy-MM-dd") print new DateTime
              Ok.chunked(Env.api.pdnDump.exportUserGames(config)).withHeaders(
                CONTENT_TYPE -> pdnContentType,
                CONTENT_DISPOSITION -> ("attachment; filename=" + s"lidraughts_${user.username}_$date.pdn")
              ).fuccess
            }
          }
        }
      }
    }

  private[controllers] def preloadUsers(game: GameModel): Funit =
    Env.user.lightUserApi preloadMany game.userIds
}

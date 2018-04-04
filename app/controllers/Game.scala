package controllers

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.mvc.RequestHeader
import scala.concurrent.duration._

import lidraughts.api.PdnDump
import lidraughts.app._
import lidraughts.common.MaxPerSecond
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
          ExportRateLimitPerUser(user.id, cost = 1) {
            val since = getLong("since", req) map { ts => new DateTime(ts) }
            val until = getLong("until", req) map { ts => new DateTime(ts) }
            val moves = getBoolOpt("moves", req) | true
            val tags = getBoolOpt("tags", req) | true
            val clocks = getBoolOpt("clocks", req) | false
            val max = getInt("max", req) map (_ atLeast 1)
            val perSecond = MaxPerSecond(me match {
              case None => 10
              case Some(m) if m is user.id => 50
              case Some(_) => 20
            })
            val formatFlags = lidraughts.game.PdnDump.WithFlags(moves = moves, tags = tags, clocks = clocks, draughtsResult = draughtsResult)
            val config = PdnDump.Config(user, since, until, max, formatFlags, perSecond)
            val date = (DateTimeFormat forPattern "yyyy-MM-dd") print new DateTime
            Ok.chunked(Env.api.pdnDump.exportUserGames(config)).withHeaders(
              CONTENT_TYPE -> pdnContentType,
              CONTENT_DISPOSITION -> ("attachment; filename=" + s"lidraughts_${user.username}_$date.pdn")
            ).fuccess
          }
        }
      }
    }

  private val ExportRateLimitPerUser = new lidraughts.memo.RateLimit[lidraughts.user.User.ID](
    credits = 10,
    duration = 1 day,
    name = "game export per user",
    key = "game_export.user"
  )

  private[controllers] def preloadUsers(game: GameModel): Funit =
    Env.user.lightUserApi preloadMany game.userIds
}

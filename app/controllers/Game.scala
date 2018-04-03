package controllers

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import scala.concurrent.duration._
import play.api.mvc.RequestHeader

import lidraughts.app._
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

  def exportForm = Auth { implicit ctx => me =>
    Env.security.forms.emptyWithCaptcha map {
      case (form, captcha) => Ok(html.game.export(form, captcha))
    }
  }

  def exportConfirm = AuthBody { implicit ctx => me =>
    implicit val req = ctx.body
    Env.security.forms.empty.bindFromRequest.fold(
      err => Env.security.forms.anyCaptcha map { captcha =>
        BadRequest(html.game.export(err, captcha))
      },
      _ => streamGamesPdn(req, me, since = none, until = none, ctx.pref.draughtsResult)
    )
  }

  def exportApi = Scoped(_.Game.Read) { req => me =>
    val since = getLong("since", req) map { ts => new DateTime(ts) }
    val until = getLong("until", req) map { ts => new DateTime(ts) }
    val max = getInt("max", req)
    streamGamesPdn(req, me, since, until, lidraughts.pref.Pref.default.draughtsResult, max)
  }

  private val ExportRateLimitPerUser = new lidraughts.memo.RateLimit[lidraughts.user.User.ID](
    credits = 10,
    duration = 1 day,
    name = "game export per user",
    key = "game_export.user"
  )

  private def streamGamesPdn(req: RequestHeader, user: lidraughts.user.User, since: Option[DateTime], until: Option[DateTime], draughtsResult: Boolean, max: Option[Int] = None) =
    RequireHttp11(req) {
      ExportRateLimitPerUser(user.id, cost = 1) {
        val date = (DateTimeFormat forPattern "yyyy-MM-dd") print new DateTime
        Ok.chunked(Env.api.pdnDump.exportUserGames(user.id, since, until, max | Int.MaxValue, draughtsResult)).withHeaders(
          CONTENT_TYPE -> pdnContentType,
          CONTENT_DISPOSITION -> ("attachment; filename=" + s"lidraughts_${user.username}_$date.pdn")
        )
      } fuccess
    }

  private[controllers] def preloadUsers(game: GameModel): Funit =
    Env.user.lightUserApi preloadMany game.userIds
}

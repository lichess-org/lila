package controllers

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import scala.concurrent.duration._
import play.api.mvc.RequestHeader

import lila.app._
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
      _ => streamGamesPgn(req, me, since = none, until = none)
    )
  }

  def exportApi = Scoped(_.Game.Read) { req => me =>
    val since = getLong("since", req) map { ts => new DateTime(ts) }
    val until = getLong("until", req) map { ts => new DateTime(ts) }
    val max = getInt("max", req)
    streamGamesPgn(req, me, since, until, max)
  }

  private val ExportRateLimitPerUser = new lila.memo.RateLimit[lila.user.User.ID](
    credits = 10,
    duration = 1 day,
    name = "game export per user",
    key = "game_export.user"
  )

  private def streamGamesPgn(req: RequestHeader, user: lila.user.User, since: Option[DateTime], until: Option[DateTime], max: Option[Int] = None) =
    RequireHttp11(req) {
      ExportRateLimitPerUser(user.id, cost = 1) {
        val date = (DateTimeFormat forPattern "yyyy-MM-dd") print new DateTime
        Ok.chunked(Env.api.pgnDump.exportUserGames(user.id, since, until, max | Int.MaxValue)).withHeaders(
          CONTENT_TYPE -> pgnContentType,
          CONTENT_DISPOSITION -> ("attachment; filename=" + s"lichess_${user.username}_$date.pgn")
        )
      } fuccess
    }

  private[controllers] def preloadUsers(game: GameModel): Funit =
    Env.user.lightUserApi preloadMany game.userIds
}

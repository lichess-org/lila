package controllers

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
      _ => fuccess(streamGamesPgn(me))
    )
  }

  def exportApi = Auth { implicit ctx => me =>
    fuccess(streamGamesPgn(me))
  }

  private def streamGamesPgn(user: lila.user.User) = {
    import org.joda.time.DateTime
    import org.joda.time.format.DateTimeFormat
    val date = (DateTimeFormat forPattern "yyyy-MM-dd") print new DateTime
    Ok.chunked(Env.api.pgnDump exportUserGames user.id).withHeaders(
      CONTENT_TYPE -> pgnContentType,
      CONTENT_DISPOSITION -> ("attachment; filename=" + s"lichess_${user.username}_$date.pgn")
    )
  }

  private[controllers] def preloadUsers(game: GameModel): Funit =
    Env.user.lightUserApi preloadMany game.userIds
}

package controllers

import play.api.mvc._
import play.api.mvc.Results._
import play.api.http.ContentTypes._
import lila.api.Context
import lila.app._
import lila.game.{ Pov, AnonCookie }
import lila.security.Granter
import views._

private[controllers] trait TheftPrevention {

  protected def PreventTheft(pov: Pov)(ok: => Fu[Result])(implicit ctx: Context): Fu[Result] =
    isTheft(pov).fold(fuccess(Redirect(routes.Round.watcher(pov.gameId, pov.color.name))), ok)

  protected def isTheft(pov: Pov)(implicit ctx: Context) = pov.game.isPgnImport || pov.player.isAi || {
    (pov.player.userId, ctx.userId) match {
      case (Some(playerId), None) => true
      case (Some(playerId), Some(userId)) =>
        playerId != userId && !(ctx.me ?? Granter.superAdmin)
      case (None, _) =>
        lila.api.Mobile.Api.requestVersion(ctx.req).isEmpty &&
        ctx.req.cookies.get(AnonCookie.name).map(_.value) != Some(pov.playerId)
    }
  }

  protected def isMyPov(pov: Pov)(implicit ctx: Context) = !isTheft(pov)

  protected lazy val theftResponse = Unauthorized(play.api.libs.json.Json.obj(
    "error" -> "This game requires authentication"
  )) as JSON
}

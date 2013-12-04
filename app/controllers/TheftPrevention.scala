package controllers

import play.api.mvc._
import play.api.mvc.Results.Redirect

import lila.app._
import lila.game.Pov
import lila.security.Granter
import lila.user.Context
import views._

private[controllers] trait TheftPrevention {

  protected def PreventTheft(pov: Pov)(ok: ⇒ Fu[SimpleResult])(implicit ctx: Context): Fu[SimpleResult] =
    isTheft(pov).fold(fuccess(Redirect(routes.Round.watcher(pov.gameId, pov.color.name))), ok)

  protected def isTheft(pov: Pov)(implicit ctx: Context) = pov.player.isAi || {
    pov.player.userId != ctx.userId && !(ctx.me ?? Granter.superAdmin)
  }
}

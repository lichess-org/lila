package controllers

import lila.app._
import views._
import lila.user.Context
import lila.game.Pov
import lila.security.Granter

import play.api.mvc._
import play.api.mvc.Results.Redirect

private[controllers] trait TheftPrevention {

  protected def PreventTheft(pov: Pov)(ok: ⇒ Result)(implicit ctx: Context): Result =
    isTheft(pov).fold(Redirect(routes.Round.watcher(pov.gameId, pov.color.name)), ok)

  protected def isTheft(pov: Pov)(implicit ctx: Context) =
    pov.player.userId != ctx.userId && !(ctx.me zmap Granter.superAdmin)
}

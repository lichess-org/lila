package controllers

import lila._
import views._
import http.Context
import game.{ DbGame, Pov }

import play.api.mvc._
import play.api.mvc.Results.Redirect

trait TheftPrevention {

  protected def PreventTheft(pov: Pov)(ok: ⇒ Result)(implicit ctx: Context): Result =
    isTheft(pov).fold(Redirect(routes.Round.watcher(pov.gameId, pov.color.name)), ok)

  protected def isTheft(pov: Pov)(implicit ctx: Context) =
    pov.player.userId != ctx.userId 
}

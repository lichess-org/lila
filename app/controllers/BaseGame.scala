package controllers

import lila.app._
import lila.api.Context
import lila.game.ListMenu
import views._

import play.api.mvc.Result

private[controllers] trait BaseGame { self: LilaController =>

  protected def makeListMenu(implicit ctx: Context): Fu[ListMenu] =
    Env.game.listMenu(
      Env.bookmark.api countByUser _, 
      Env.analyse.cached.nbAnalysis _, 
      ctx.me)
}

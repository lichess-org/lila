package controllers

import lila.app._
import views._
import lila.user.Context

import play.api.mvc.Result

private[controllers] trait BaseGame { self: LilaController ⇒

  protected def nbAnalysis = Env.analyse.cached.nbAnalysis _
  protected def bookmarkApi = Env.bookmark.api
  protected def listMenu = Env.game.listMenu

  protected def makeListMenu(implicit ctx: Context) =
    listMenu(bookmarkApi countByUser _, nbAnalysis, ctx.me)
}

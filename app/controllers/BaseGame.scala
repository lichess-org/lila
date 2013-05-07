package controllers

import lila.app._
import views._
import lila.user.Context

import play.api.mvc.Result

private[controllers] trait BaseGame { self: LilaController ⇒

  protected def nbAnalysis = Env.analyse.cached.nbAnalysis _
  protected def bookmarkApi = Env.bookmark.api
  protected def listMenu = Env.game.listMenu

  private val maxPage = 40

  protected def Reasonable(page: Int)(result: ⇒ Fu[Result]): Fu[Result] =
    (page < maxPage).fold(result, BadRequest("resource too old").fuccess)

  protected def makeListMenu(implicit ctx: Context) =
    listMenu(bookmarkApi countByUser _, nbAnalysis, ctx.me)
}

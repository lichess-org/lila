package controllers

import lila.app._
import views._
import http.Context

import play.api.mvc.Result

trait BaseGame { self: LilaController =>

  protected def gameRepo = env.game.gameRepo
  protected def analyseCached = env.analyse.cached
  protected def bookmarkApi = env.bookmark.api
  protected def listMenu = env.game.listMenu

  private val maxPage = 40

  protected def reasonable(page: Int)(result: ⇒ Result): Result =
    (page < maxPage).fold(result, BadRequest("too old"))

  protected def makeListMenu(implicit ctx: Context) =
    listMenu(bookmarkApi.countByUser, analyseCached.nbAnalysis, ctx.me)
}

package controllers

import lila._
import views._
import http.Context

import play.api.mvc.Result

trait BaseGame { self: LilaController =>

  def gameRepo = env.game.gameRepo
  def analyseCached = env.analyse.cached
  def bookmarkApi = env.bookmark.api
  def listMenu = env.game.listMenu

  val maxPage = 40

  def reasonable(page: Int)(result: ⇒ Result): Result =
    (page < maxPage).fold(result, BadRequest("too old"))

  def makeListMenu(implicit ctx: Context) =
    listMenu(bookmarkApi.countByUser, analyseCached.nbAnalysis, ctx.me)
}

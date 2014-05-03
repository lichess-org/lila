package controllers

import lila.app._
import lila.game.{ Game => GameModel, GameRepo }
import views._

object Game extends LilaController with BaseGame {

  private def paginator = Env.game.paginator
  private def analysePaginator = Env.analyse.paginator
  private def cached = Env.game.cached
  private def searchEnv = Env.gameSearch
  def searchForm = searchEnv.forms.search

  def search(page: Int) = OpenBody { implicit ctx =>
    Reasonable(page, 100) {
      implicit def req = ctx.body
      makeListMenu flatMap { listMenu =>
        searchForm.bindFromRequest.fold(
          failure => Ok(html.game.search(listMenu, failure)).fuccess,
          data => searchEnv.nonEmptyQuery(data) ?? { query =>
            searchEnv.paginator(query, page) map (_.some)
          } map { pager =>
            Ok(html.game.search(listMenu, searchForm fill data, pager))
          }
        )
      }
    }
  }

  def realtime = Open { implicit ctx =>
    GameRepo.featuredCandidates map lila.tv.Featured.sort map (_ take 9) zip
      makeListMenu map {
        case (games, menu) => html.game.realtime(games, menu)
      }
  }

  def all(page: Int) = Open { implicit ctx =>
    Reasonable(page) {
      paginator recent page zip makeListMenu map {
        case (pag, menu) => html.game.all(pag, menu)
      }
    }
  }

  def checkmate(page: Int) = Open { implicit ctx =>
    Reasonable(page) {
      paginator checkmate page zip makeListMenu map {
        case (pag, menu) => html.game.checkmate(pag, menu)
      }
    }
  }

  def bookmark(page: Int) = Auth { implicit ctx =>
    me =>
      Reasonable(page) {
        Env.bookmark.api.gamePaginatorByUser(me, page) zip makeListMenu map {
          case (pag, menu) => html.game.bookmarked(pag, menu)
        }
      }
  }

  def popular(page: Int) = Open { implicit ctx =>
    Reasonable(page) {
      paginator popular page zip makeListMenu map {
        case (pag, menu) => html.game.popular(pag, menu)
      }
    }
  }

  def analysed(page: Int) = Open { implicit ctx =>
    Reasonable(page) {
      analysePaginator games page zip makeListMenu map {
        case (pag, menu) => html.game.analysed(pag, menu)
      }
    }
  }

  def imported(page: Int) = Open { implicit ctx =>
    Reasonable(page) {
      paginator imported page zip makeListMenu map {
        case (pag, menu) => html.game.imported(pag, menu)
      }
    }
  }
}

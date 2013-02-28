package controllers

import lila.app._
import views._
import http.Context

import play.api.mvc.Result

object Game extends LilaController with BaseGame {

  private def paginator = env.game.paginator
  private def analysePaginator = env.analyse.paginator
  private def cached = env.game.cached

  val realtime = Open { implicit ctx ⇒
    IOk(gameRepo recentGames 9 map { games ⇒
      html.game.realtime(games, makeListMenu)
    })
  }

  def all(page: Int) = Open { implicit ctx ⇒
    reasonable(page) {
      Ok(html.game.all(paginator recent page, makeListMenu))
    }
  }

  def checkmate(page: Int) = Open { implicit ctx ⇒
    reasonable(page) {
      Ok(html.game.checkmate(paginator checkmate page, makeListMenu))
    }
  }

  def bookmark(page: Int) = Auth { implicit ctx ⇒
    me ⇒
      reasonable(page) {
        Ok(html.game.bookmarked(bookmarkApi.gamePaginatorByUser(me, page), makeListMenu))
      }
  }

  def popular(page: Int) = Open { implicit ctx ⇒
    reasonable(page) {
      Ok(html.game.popular(paginator popular page, makeListMenu))
    }
  }

  def analysed(page: Int) = Open { implicit ctx ⇒
    reasonable(page) {
      Ok(html.game.analysed(analysePaginator games page, makeListMenu))
    }
  }

  def imported(page: Int) = Open { implicit ctx ⇒
    reasonable(page) {
      Ok(html.game.imported(paginator imported page, makeListMenu))
    }
  }
}

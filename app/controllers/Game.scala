package controllers

import lila.app._
import lila.game.{ Game ⇒ GameModel, GameRepo }
import views._

object Game extends LilaController with BaseGame {

  private def paginator = Env.game.paginator
  private def analysePaginator = Env.analyse.paginator
  private def cached = Env.game.cached

  def realtime = Open { implicit ctx ⇒
    GameRepo recentGames 9 zip makeListMenu map {
      case (games, menu) ⇒ html.game.realtime(games, menu)
    }
  }

  def all(page: Int) = Open { implicit ctx ⇒
    Reasonable(page) {
      paginator recent page zip makeListMenu map {
        case (pag, menu) ⇒ html.game.all(pag, menu)
      }
    }
  }

  def checkmate(page: Int) = Open { implicit ctx ⇒
    Reasonable(page) {
      paginator checkmate page zip makeListMenu map {
        case (pag, menu) ⇒ html.game.checkmate(pag, menu)
      }
    }
  }

  def bookmark(page: Int) = Auth { implicit ctx ⇒
    me ⇒
      Reasonable(page) {
        bookmarkApi.gamePaginatorByUser(me, page) zip makeListMenu map {
          case (pag, menu) ⇒ html.game.bookmarked(pag, menu)
        }
      }
  }

  def popular(page: Int) = Open { implicit ctx ⇒
    Reasonable(page) {
      paginator popular page zip makeListMenu map {
        case (pag, menu) ⇒ html.game.popular(pag, menu)
      }
    }
  }

  def analysed(page: Int) = Open { implicit ctx ⇒
    Reasonable(page) {
      analysePaginator games page zip makeListMenu map {
        case (pag, menu) ⇒ html.game.analysed(pag, menu)
      }
    }
  }

  def imported(page: Int) = Open { implicit ctx ⇒
    Reasonable(page) {
      paginator imported page zip makeListMenu map {
        case (pag, menu) ⇒ html.game.imported(pag, menu)
      }
    }
  }
}

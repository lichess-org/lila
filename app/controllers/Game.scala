package controllers

import lila._
import views._
import http.Context

import play.api.mvc.Result

object Game extends LilaController {

  val gameRepo = env.game.gameRepo
  val paginator = env.game.paginator
  val cached = env.game.cached
  val starApi = env.star.api
  val listMenu = env.game.listMenu

  val maxPage = 40

  val realtime = Open { implicit ctx ⇒
    IOk(for {
      games ← gameRepo recentGames 9
      menu ← makeListMenu
    } yield html.game.realtime(games, menu))
  }

  def realtimeInner(ids: String) = Open { implicit ctx ⇒
    IOk(gameRepo games ids.split(",").toList map { games ⇒
      html.game.realtimeInner(games)
    })
  }

  def all(page: Int) = Open { implicit ctx ⇒
    reasonable(page) {
      IOk(makeListMenu map { menu ⇒
        html.game.all(paginator recent page, menu)
      })
    }
  }

  def checkmate(page: Int) = Open { implicit ctx ⇒
    reasonable(page) {
      IOk(makeListMenu map { menu ⇒
        html.game.checkmate(paginator checkmate page, menu)
      })
    }
  }

  def star(page: Int) = Auth { implicit ctx ⇒
    me ⇒
      reasonable(page) {
        IOk(makeListMenu map { menu ⇒
          html.game.star(starApi.gamePaginatorByUser(me, page), menu)
        })
      }
  }

  def popular(page: Int) = Open { implicit ctx ⇒
    reasonable(page) {
      IOk(makeListMenu map { menu ⇒
        html.game.popular(paginator popular page, menu)
      })
    }
  }

  private def reasonable(page: Int)(result: ⇒ Result): Result =
    (page < maxPage).fold(result, BadRequest("too old"))

  private def makeListMenu(implicit ctx: Context) =
    listMenu(starApi.countByUser, ctx.me)
}

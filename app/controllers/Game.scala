package controllers

import lila._
import views._

object Game extends LilaController {

  val gameRepo = env.game.gameRepo
  val paginator = env.game.paginator
  val cached = env.game.cached

  val realtime = Open { implicit ctx ⇒
    IOk(gameRepo recentGames 9 map { games ⇒
      html.game.realtime(games, cached.nbGames, cached.nbMates)
    })
  }

  def realtimeInner(ids: String) = Open { implicit ctx ⇒
    IOk(gameRepo games ids.split(",").toList map { games ⇒
      html.game.realtimeInner(games)
    })
  }

  def all(page: Int) = Open { implicit ctx ⇒
    Ok(html.game.all(
      paginator recent page, cached.nbGames, cached.nbMates
    ))
  }

  def checkmate(page: Int) = Open { implicit ctx ⇒
    Ok(html.game.checkmate(
      paginator checkmate page, cached.nbGames, cached.nbMates
    ))
  }
}

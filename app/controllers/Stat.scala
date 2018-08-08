package controllers

import lidraughts.app._
import views._

object Stat extends LidraughtsController {

  def ratingDistribution(perfKey: lidraughts.rating.Perf.Key) = Open { implicit ctx =>
    lidraughts.rating.PerfType(perfKey).filter(lidraughts.rating.PerfType.leaderboardable.has) match {
      case Some(perfType) => Env.user.cached.ratingDistribution(perfType) map { data =>
        Ok(html.stat.ratingDistribution(perfType, data))
      }
      case _ => notFound
    }
  }
}

package controllers

import lila.app._
import views._

final class Stat(env: Env) extends LilaController(env) {

  def ratingDistribution(perfKey: lila.rating.Perf.Key) =
    Open { implicit ctx =>
      lila.rating.PerfType(perfKey).filter(lila.rating.PerfType.leaderboardable.has) match {
        case Some(perfType) =>
          env.user.rankingApi.monthlyRatingDistribution(perfType) dmap { data =>
            Ok(html.stat.ratingDistribution(perfType, data))
          }
        case _ => notFound
      }
    }
}

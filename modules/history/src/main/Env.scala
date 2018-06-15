package lila.history

import com.typesafe.config.Config

final class Env(
    config: Config,
    mongoCache: lila.memo.MongoCache.Builder,
    db: lila.db.Env
) {

  private val CachedRatingChartTtl = config duration "cached.rating_chart.ttl"

  private val Collectionhistory = config getString "collection.history"

  lazy val api = new HistoryApi(db(Collectionhistory))

  lazy val ratingChartApi = new RatingChartApi(
    historyApi = api,
    mongoCache = mongoCache,
    cacheTtl = CachedRatingChartTtl
  )
}

object Env {

  lazy val current = "history" boot new Env(
    config = lila.common.PlayApp loadConfig "history",
    mongoCache = lila.memo.Env.current.mongoCache,
    db = lila.db.Env.current
  )
}

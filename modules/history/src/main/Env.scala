package lila.history

import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import scala.concurrent.duration.FiniteDuration

import lila.common.config._

@Module
private class HistoryConfig(
    @ConfigName("collection.history") val historyColl: CollName,
    @ConfigName("cached.rating_chart.ttl") val ratingChartTtl: FiniteDuration
)

final class Env(
    appConfig: Configuration,
    mongoCache: lila.memo.MongoCache.Builder,
    db: lila.db.Env
) {

  private val config = appConfig.get[HistoryConfig]("history")(AutoConfig.loader)

  private lazy val coll = db(config.historyColl)

  lazy val api = wire[HistoryApi]

  lazy val ratingChartApi = wire[RatingChartApi]
}

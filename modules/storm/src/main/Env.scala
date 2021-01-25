package lila.storm

import com.softwaremill.macwire._
import play.api.Configuration

import lila.common.config._

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    colls: lila.puzzle.PuzzleColls,
    cacheApi: lila.memo.CacheApi
)(implicit
    ec: scala.concurrent.ExecutionContext
) {

  private lazy val runColl = db(CollName("storm_run"))

  lazy val selector = wire[StormSelector]

  lazy val json = wire[StormJson]

  lazy val runApi = wire[StormRunApi]

  val forms = StormForm
}

package lila.storm

import com.softwaremill.macwire.*
import play.api.Configuration

import lila.core.config.*
import lila.common.config.given

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    colls: lila.puzzle.PuzzleColls,
    cacheApi: lila.memo.CacheApi,
    userApi: lila.core.user.UserApi
)(using Executor):

  private lazy val dayColl = db(CollName("storm_day"))

  lazy val selector = wire[StormSelector]

  private val signSecret = appConfig.get[Secret]("storm.secret")

  lazy val sign = wire[StormSign]

  lazy val json = wire[StormJson]

  lazy val highApi = wire[StormHighApi]

  lazy val dayApi = wire[StormDayApi]

  val forms = StormForm

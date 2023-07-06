package lila.storm

import com.softwaremill.macwire.*
import play.api.Configuration

import lila.common.config.*

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    colls: lila.puzzle.PuzzleColls,
    cacheApi: lila.memo.CacheApi,
    userRepo: lila.user.UserRepo,
    perfsRepo: lila.user.UserPerfsRepo
)(using Executor):

  private lazy val dayColl = db(CollName("storm_day"))

  lazy val selector = wire[StormSelector]

  private val signSecret = appConfig.get[Secret]("storm.secret")

  lazy val sign = wire[StormSign]

  lazy val json = wire[StormJson]

  lazy val highApi = wire[StormHighApi]

  lazy val dayApi = wire[StormDayApi]

  val forms = StormForm

package lila.storm

import com.softwaremill.macwire._
import play.api.Configuration

import lila.common.config._
import lila.user.UserRepo

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    colls: lila.puzzle.PuzzleColls,
    cacheApi: lila.memo.CacheApi,
    userRepo: UserRepo
)(implicit
    ec: scala.concurrent.ExecutionContext
) {

  private lazy val dayColl = db(CollName("storm_day"))

  lazy val selector = wire[StormSelector]

  private val signSecret = appConfig.get[Secret]("storm.secret")

  lazy val sign = wire[StormSign]

  lazy val json = wire[StormJson]

  lazy val highApi = wire[StormHighApi]

  lazy val dayApi = wire[StormDayApi]

  val forms = StormForm

  lila.common.Bus.subscribeFuns(
    "gdprErase" -> { case lila.user.User.GDPRErase(user) =>
      dayApi.eraseAllFor(user).unit
    }
  )
}

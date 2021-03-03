package lila.racer

import com.softwaremill.macwire._
import play.api.Configuration

import lila.common.config._
import lila.user.UserRepo
import lila.storm.StormSelector

@Module
final class Env(
    selector: StormSelector,
    db: lila.db.Db
)(implicit
    ec: scala.concurrent.ExecutionContext
) {

  private lazy val raceColl = db(CollName("racer_race"))

  lazy val api = wire[RacerApi]
}

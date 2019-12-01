package lila.coordinate

import play.api.Configuration
import com.softwaremill.macwire._

import lila.common.config.CollName

final class Env(
    appConfig: Configuration,
    db: lila.db.Env
) {

  private lazy val scoreColl = db(appConfig.get[CollName]("coordinate.collection.score"))

  lazy val api = wire[CoordinateApi]

  lazy val forms = DataForm
}

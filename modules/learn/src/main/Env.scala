package lila.learn

import io.methvin.play.autoconfig._
import play.api.Configuration

import lila.common.config._

final class Env(
    appConfig: Configuration,
    db: lila.db.Env
) {
  lazy val api = new LearnApi(
    coll = db(appConfig.get[CollName]("learn.collection.progress"))
  )
}

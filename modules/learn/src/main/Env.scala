package lila.learn

import play.api.Configuration

import lila.common.config._

final class Env(
    appConfig: Configuration,
    db: lila.db.Db
)(implicit ec: scala.concurrent.ExecutionContext) {
  lazy val api = new LearnApi(
    coll = db(appConfig.get[CollName]("learn.collection.progress"))
  )
}

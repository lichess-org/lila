package lila.learn

import play.api.Configuration

import lila.common.config.CollName
import lila.common.autoconfig.given

final class Env(
    appConfig: Configuration,
    db: lila.db.Db
)(using ec: scala.concurrent.ExecutionContext):
  lazy val api = new LearnApi(
    coll = db(appConfig.get[CollName]("learn.collection.progress"))
  )

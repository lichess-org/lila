package lila.learn

import play.api.Configuration

import lila.common.autoconfig.given
import lila.core.config.CollName

final class Env(
    appConfig: Configuration,
    db: lila.db.Db
)(using Executor):

  lazy val api = LearnApi(coll = db(appConfig.get[CollName]("learn.collection.progress")))

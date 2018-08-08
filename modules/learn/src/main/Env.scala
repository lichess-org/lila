package lidraughts.learn

import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lidraughts.db.Env
) {

  private val CollectionProgress = config getString "collection.progress"

  lazy val api = new LearnApi(
    coll = db(CollectionProgress)
  )
}

object Env {

  lazy val current: Env = "learn" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "learn",
    db = lidraughts.db.Env.current
  )
}

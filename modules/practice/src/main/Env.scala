package lila.practice

import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env) {

  private val CollectionProgress = config getString "collection.progress"

  lazy val api = new PracticeApi(
    coll = db(CollectionProgress))
}

object Env {

  lazy val current: Env = "practice" boot new Env(
    config = lila.common.PlayApp loadConfig "practice",
    db = lila.db.Env.current)
}

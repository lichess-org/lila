package lila.learn

import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env) {
}

object Env {

  lazy val current: Env = "learn" boot new Env(
    config = lila.common.PlayApp loadConfig "learn",
    db = lila.db.Env.current)
}

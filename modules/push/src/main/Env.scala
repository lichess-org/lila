package lila.push

import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env) {

  private val settings = new {
    val CollectionDevice = config getString "collection.device"
  }
  import settings._

  lazy val deviceApi = new DeviceApi(db(CollectionPlayban))
}

object Env {

  lazy val current: Env = "push" boot new Env(
    config = lila.common.PlayApp loadConfig "push",
    db = lila.db.Env.current)
}

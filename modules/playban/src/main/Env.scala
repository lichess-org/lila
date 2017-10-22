package lila.playban

import com.typesafe.config.Config

final class Env(
    config: Config,
    messenger: lila.message.MessageApi,
    bus: lila.common.Bus,
    db: lila.db.Env
) {

  private val settings = new {
    val CollectionPlayban = config getString "collection.playban"
  }
  import settings._

  lazy val api = new PlaybanApi(
    coll = db(CollectionPlayban),
    sandbag = new SandbagWatch(messenger),
    bus = bus
  )
}

object Env {

  lazy val current: Env = "playban" boot new Env(
    config = lila.common.PlayApp loadConfig "playban",
    messenger = lila.message.Env.current.api,
    bus = lila.common.PlayApp.system.lilaBus,
    db = lila.db.Env.current
  )
}

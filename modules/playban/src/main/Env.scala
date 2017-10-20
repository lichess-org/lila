package lila.playban

import com.typesafe.config.Config

final class Env(
    config: Config,
    isRematch: String => Boolean,
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
    isRematch = isRematch,
    bus = bus
  )
}

object Env {

  lazy val current: Env = "playban" boot new Env(
    config = lila.common.PlayApp loadConfig "playban",
    isRematch = lila.game.Env.current.cached.isRematch.get _,
    messenger = lila.message.Env.current.api,
    bus = lila.common.PlayApp.system.lilaBus,
    db = lila.db.Env.current
  )
}

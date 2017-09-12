package lila.playban

import com.typesafe.config.Config

final class Env(
    config: Config,
    isRematch: String => Boolean,
    messenger: lila.message.MessageApi,
    db: lila.db.Env
) {

  private val settings = new {
    val CollectionPlayban = config getString "collection.playban"
  }
  import settings._

  lazy val api = new PlaybanApi(
    coll = db(CollectionPlayban),
    sandbag = new SandbagWatch(messenger),
    isRematch = isRematch
  )
}

object Env {

  lazy val current: Env = "playban" boot new Env(
    config = lila.common.PlayApp loadConfig "playban",
    isRematch = lila.game.Env.current.cached.isRematch.get _,
    messenger = lila.message.Env.current.api,
    db = lila.db.Env.current
  )
}

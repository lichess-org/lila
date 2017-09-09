package lila.event

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env,
    asyncCache: lila.memo.AsyncCache.Builder,
    system: ActorSystem
) {

  private val CollectionEvent = config getString "collection.event"

  private lazy val eventColl = db(CollectionEvent)

  lazy val api = new EventApi(coll = eventColl, asyncCache = asyncCache)
}

object Env {

  lazy val current = "event" boot new Env(
    config = lila.common.PlayApp loadConfig "event",
    db = lila.db.Env.current,
    asyncCache = lila.memo.Env.current.asyncCache,
    system = lila.common.PlayApp.system
  )
}

package lidraughts.event

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lidraughts.db.Env,
    asyncCache: lidraughts.memo.AsyncCache.Builder,
    system: ActorSystem
) {

  private val CollectionEvent = config getString "collection.event"

  private lazy val eventColl = db(CollectionEvent)

  lazy val api = new EventApi(coll = eventColl, asyncCache = asyncCache)
}

object Env {

  lazy val current = "event" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "event",
    db = lidraughts.db.Env.current,
    asyncCache = lidraughts.memo.Env.current.asyncCache,
    system = lidraughts.common.PlayApp.system
  )
}

package lila.message

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env,
    mongoCache: lila.memo.MongoCache.Builder,
    shutup: ActorSelection,
    notifyApi: lila.notify.NotifyApi,
    blocks: (String, String) => Fu[Boolean],
    follows: (String, String) => Fu[Boolean],
    getPref: String => Fu[lila.pref.Pref],
    system: ActorSystem) {

  private val CollectionThread = config getString "collection.thread"
  private val ThreadMaxPerPage = config getInt "thread.max_per_page"

  private[message] lazy val threadColl = db(CollectionThread)

  lazy val forms = new DataForm(security = security)

  lazy val api = new Api(
    coll = threadColl,
    shutup = shutup,
    maxPerPage = ThreadMaxPerPage,
    blocks = blocks,
    notifyApi = notifyApi,
    follows = follows)

  lazy val security = new MessageSecurity(
    follows = follows,
    blocks = blocks,
    getPref = getPref)
}

object Env {

  lazy val current = "message" boot new Env(
    config = lila.common.PlayApp loadConfig "message",
    db = lila.db.Env.current,
    shutup = lila.hub.Env.current.actor.shutup,
    mongoCache = lila.memo.Env.current.mongoCache,
    notifyApi = lila.notify.Env.current.api,
    blocks = lila.relation.Env.current.api.fetchBlocks,
    follows = lila.relation.Env.current.api.fetchFollows,
    getPref = lila.pref.Env.current.api.getPref,
    system = lila.common.PlayApp.system)
}

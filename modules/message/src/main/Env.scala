package lila.message

import akka.actor._
import com.typesafe.config.Config

import lila.hub.actorApi.message.LichessThread

final class Env(
    config: Config,
    db: lila.db.Env,
    mongoCache: lila.memo.MongoCache.Builder,
    shutup: ActorSelection,
    blocks: (String, String) => Fu[Boolean],
    follows: (String, String) => Fu[Boolean],
    getPref: String => Fu[lila.pref.Pref],
    system: ActorSystem) {

  private val CollectionThread = config getString "collection.thread"
  private val ThreadMaxPerPage = config getInt "thread.max_per_page"
  private val ActorName = config getString "actor.name"

  import scala.collection.JavaConversions._
  val LichessSenders = (config getStringList "lichess_senders").toList

  private[message] lazy val threadColl = db(CollectionThread)

  private lazy val unreadCache = new UnreadCache(mongoCache)

  lazy val forms = new DataForm(security = security)

  lazy val api = new Api(
    unreadCache = unreadCache,
    shutup = shutup,
    maxPerPage = ThreadMaxPerPage,
    blocks = blocks,
    bus = system.lilaBus)

  lazy val security = new MessageSecurity(
    follows = follows,
    blocks = blocks,
    getPref = getPref)

  system.actorOf(Props(new Actor {
    def receive = {
      case thread: LichessThread => api.lichessThread(thread)
    }
  }), name = ActorName)
}

object Env {

  lazy val current = "message" boot new Env(
    config = lila.common.PlayApp loadConfig "message",
    db = lila.db.Env.current,
    shutup = lila.hub.Env.current.actor.shutup,
    mongoCache = lila.memo.Env.current.mongoCache,
    blocks = lila.relation.Env.current.api.fetchBlocks,
    follows = lila.relation.Env.current.api.fetchFollows,
    getPref = lila.pref.Env.current.api.getPref,
    system = lila.common.PlayApp.system)
}

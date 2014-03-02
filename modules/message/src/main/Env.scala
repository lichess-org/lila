package lila.message

import akka.actor._
import com.typesafe.config.Config

import lila.hub.actorApi.message.LichessThread

final class Env(
    config: Config,
    db: lila.db.Env,
    blocks: (String, String) => Fu[Boolean],
    system: ActorSystem) {

  private val CollectionThread = config getString "collection.thread"
  private val ThreadMaxPerPage = config getInt "thread.max_per_page"
  private val ActorName = config getString "actor.name"

  private[message] lazy val threadColl = db(CollectionThread)

  private lazy val unreadCache = new UnreadCache

  lazy val forms = new DataForm(blocks = blocks)

  lazy val api = new Api(
    unreadCache = unreadCache,
    maxPerPage = ThreadMaxPerPage,
    bus = system.lilaBus)

  system.actorOf(Props(new Actor {
    def receive = {
      case thread: LichessThread => api.lichessThread(thread)
    }
  }), name = ActorName)

  def cli = new lila.common.Cli {
    import tube.threadTube
    def process = {
      case "message" :: "typecheck" :: Nil => lila.db.Typecheck.apply[Thread]
    }
  }
}

object Env {

  lazy val current = "[boot] message" describes new Env(
    config = lila.common.PlayApp loadConfig "message",
    db = lila.db.Env.current,
    blocks = lila.relation.Env.current.api.blocks,
    system = lila.common.PlayApp.system)
}

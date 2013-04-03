package lila.message

import lila.hub.actorApi.message.LichessThread

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env,
    sockets: ActorRef, 
    system: ActorSystem) {

  private val CollectionThread = config getString "collection.thread"
  private val ThreadMaxPerPage = config getInt "thread.max_per_page"
  private val ActorName = config getString "actor.name"

  private[message] lazy val threadColl = db(CollectionThread)

  private lazy val unreadCache = new UnreadCache

  lazy val forms = new DataForm

  lazy val api = new Api(
    unreadCache = unreadCache,
    maxPerPage = ThreadMaxPerPage,
    sockets = sockets)

  private val actor = system.actorOf(Props(new Actor {
    def receive = {
      case thread: LichessThread ⇒ api.lichessThread(thread)
    }
  }), name = ActorName)

  def cli = new Cli(this)
}

object Env {

  lazy val current = "[message] boot" describes new Env(
    config = lila.common.PlayApp loadConfig "message",
    db = lila.db.Env.current,
    sockets = lila.hub.Env.current.sockets,
    system = lila.common.PlayApp.system)
}

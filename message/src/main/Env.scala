package lila.message

import lila.db.Types.Coll

import akka.actor.ActorRef
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env,
    sockets: ActorRef) {

  val CollectionThread = config getString "collection.thread"
  val ThreadMaxPerPage = config getInt "thread.max_per_page"

  private[message] lazy val threadColl = db(CollectionThread)

  private lazy val unreadCache = new UnreadCache

  lazy val forms = new DataForm

  lazy val api = new Api(
    unreadCache = unreadCache,
    maxPerPage = ThreadMaxPerPage,
    sockets = sockets)

  def cli = new Cli(this)
}

object Env {

  lazy val current = "[message] boot" describes new Env(
    config = lila.common.PlayApp loadConfig "message",
    db = lila.db.Env.current,
    sockets = lila.hub.Env.current.sockets)
}

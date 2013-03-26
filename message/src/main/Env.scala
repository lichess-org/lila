package lila.message

import lila.db.Types.ReactiveColl
import lila.user.UserRepo

import akka.actor.ActorRef
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env,
    userRepo: UserRepo,
    sockets: ActorRef) {

  val CollectionThread = config getString "collection.thread"
  val ThreadMaxPerPage = config getInt "thread.max_per_page"

  private lazy val threadRepo = new ThreadRepo(db(CollectionThread))

  private lazy val unreadCache = new UnreadCache(threadRepo)

  lazy val forms = new DataForm(userRepo)

  lazy val api = new Api(
    threadRepo = threadRepo,
    unreadCache = unreadCache,
    userRepo = userRepo,
    maxPerPage = ThreadMaxPerPage,
    sockets = sockets)

  def cli = new Cli(this)
}

object Env {

  lazy val current = new Env(
    config = lila.common.PlayApp loadConfig "message",
    db = lila.db.Env.current,
    userRepo = lila.user.Env.current.userRepo,
    sockets = lila.hub.Env.current.sockets)
}

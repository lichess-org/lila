package lidraughts.message

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lidraughts.db.Env,
    shutup: ActorSelection,
    notifyApi: lidraughts.notify.NotifyApi,
    blocks: (String, String) => Fu[Boolean],
    follows: (String, String) => Fu[Boolean],
    getPref: String => Fu[lidraughts.pref.Pref],
    system: ActorSystem,
    isOnline: lidraughts.user.User.ID => Boolean,
    lightUser: lidraughts.common.LightUser.GetterSync
) {

  private val CollectionThread = config getString "collection.thread"
  private val ThreadMaxPerPage = config getInt "thread.max_per_page"

  private[message] lazy val threadColl = db(CollectionThread)

  lazy val forms = new DataForm(security = security)

  lazy val jsonView = new JsonView(isOnline, lightUser)

  lazy val batch = new MessageBatch(
    coll = threadColl,
    notifyApi = notifyApi
  )

  lazy val api = new MessageApi(
    coll = threadColl,
    shutup = shutup,
    maxPerPage = lidraughts.common.MaxPerPage(ThreadMaxPerPage),
    blocks = blocks,
    notifyApi = notifyApi,
    security = security,
    lidraughtsBus = system.lidraughtsBus
  )

  lazy val security = new MessageSecurity(
    follows = follows,
    blocks = blocks,
    getPref = getPref
  )
}

object Env {

  lazy val current = "message" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "message",
    db = lidraughts.db.Env.current,
    shutup = lidraughts.hub.Env.current.actor.shutup,
    notifyApi = lidraughts.notify.Env.current.api,
    blocks = lidraughts.relation.Env.current.api.fetchBlocks,
    follows = lidraughts.relation.Env.current.api.fetchFollows,
    getPref = lidraughts.pref.Env.current.api.getPref,
    system = lidraughts.common.PlayApp.system,
    isOnline = lidraughts.user.Env.current.isOnline,
    lightUser = lidraughts.user.Env.current.lightUserSync
  )
}

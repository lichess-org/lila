package lila.message

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env,
    shutup: ActorSelection,
    notifyApi: lila.notify.NotifyApi,
    blocks: (String, String) => Fu[Boolean],
    follows: (String, String) => Fu[Boolean],
    getPref: String => Fu[lila.pref.Pref],
    spam: lila.security.Spam,
    system: ActorSystem,
    isOnline: lila.user.User.ID => Boolean,
    lightUser: lila.common.LightUser.GetterSync
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
    maxPerPage = lila.common.MaxPerPage(ThreadMaxPerPage),
    blocks = blocks,
    notifyApi = notifyApi,
    security = security,
    lilaBus = system.lilaBus
  )

  lazy val security = new MessageSecurity(
    follows = follows,
    blocks = blocks,
    getPref = getPref,
    spam = spam
  )

  system.lilaBus.subscribeFun('gdprErase) {
    case lila.user.User.GDPRErase(user) => api erase user
  }
}

object Env {

  lazy val current = "message" boot new Env(
    config = lila.common.PlayApp loadConfig "message",
    db = lila.db.Env.current,
    shutup = lila.hub.Env.current.shutup,
    notifyApi = lila.notify.Env.current.api,
    blocks = lila.relation.Env.current.api.fetchBlocks,
    follows = lila.relation.Env.current.api.fetchFollows,
    getPref = lila.pref.Env.current.api.getPref,
    spam = lila.security.Env.current.spam,
    system = lila.common.PlayApp.system,
    isOnline = lila.user.Env.current.isOnline,
    lightUser = lila.user.Env.current.lightUserSync
  )
}

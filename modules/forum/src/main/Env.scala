package lila.forum

import akka.actor._
import com.typesafe.config.Config

import lila.common.{ DetectLanguage, MaxPerPage }

import lila.hub.actorApi.team.CreateTeam
import lila.mod.ModlogApi
import lila.notify.NotifyApi
import lila.relation.RelationApi

final class Env(
    config: Config,
    db: lila.db.Env,
    modLog: ModlogApi,
    spam: lila.security.Spam,
    shutup: ActorSelection,
    hub: lila.hub.Env,
    detectLanguage: DetectLanguage,
    notifyApi: NotifyApi,
    relationApi: RelationApi,
    asyncCache: lila.memo.AsyncCache.Builder,
    system: ActorSystem
) {

  private val settings = new {
    val TopicMaxPerPage = config getInt "topic.max_per_page"
    val PostMaxPerPage = config getInt "post.max_per_page"
    val RecentTtl = config duration "recent.ttl"
    val RecentNb = config getInt "recent.nb"
    val CollectionCateg = config getString "collection.categ"
    val CollectionTopic = config getString "collection.topic"
    val CollectionPost = config getString "collection.post"
    import scala.collection.JavaConversions._
    val PublicCategIds = (config getStringList "public_categ_ids").toList
  }
  import settings._

  lazy val categApi = new CategApi(env = this)

  lazy val mentionNotifier = new MentionNotifier(notifyApi = notifyApi, relationApi = relationApi)

  lazy val topicApi = new TopicApi(
    env = this,
    indexer = hub.forumSearch,
    maxPerPage = MaxPerPage(TopicMaxPerPage),
    modLog = modLog,
    spam = spam,
    shutup = shutup,
    timeline = hub.timeline,
    detectLanguage = detectLanguage,
    mentionNotifier = mentionNotifier
  )

  lazy val postApi = new PostApi(
    env = this,
    indexer = hub.forumSearch,
    maxPerPage = MaxPerPage(PostMaxPerPage),
    modLog = modLog,
    spam = spam,
    shutup = shutup,
    timeline = hub.timeline,
    detectLanguage = detectLanguage,
    mentionNotifier = mentionNotifier
  )

  lazy val forms = new DataForm(hub.captcher)
  lazy val recent = new Recent(postApi, RecentTtl, RecentNb, asyncCache, PublicCategIds)

  lila.common.Bus.subscribeFun('team, 'gdprErase) {
    case CreateTeam(id, name, _) => categApi.makeTeam(id, name)
    case lila.user.User.GDPRErase(user) => postApi erase user
  }

  private[forum] lazy val categColl = db(CollectionCateg)
  private[forum] lazy val topicColl = db(CollectionTopic)
  private[forum] lazy val postColl = db(CollectionPost)
}

object Env {

  lazy val current = "forum" boot new Env(
    config = lila.common.PlayApp loadConfig "forum",
    db = lila.db.Env.current,
    modLog = lila.mod.Env.current.logApi,
    spam = lila.security.Env.current.spam,
    shutup = lila.hub.Env.current.shutup,
    hub = lila.hub.Env.current,
    detectLanguage = DetectLanguage(lila.common.PlayApp loadConfig "detectlanguage"),
    notifyApi = lila.notify.Env.current.api,
    relationApi = lila.relation.Env.current.api,
    asyncCache = lila.memo.Env.current.asyncCache,
    system = lila.common.PlayApp.system
  )
}

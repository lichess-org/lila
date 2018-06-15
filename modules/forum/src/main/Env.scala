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
    indexer = hub.actor.forumSearch,
    maxPerPage = MaxPerPage(TopicMaxPerPage),
    modLog = modLog,
    shutup = shutup,
    timeline = hub.actor.timeline,
    detectLanguage = detectLanguage,
    mentionNotifier = mentionNotifier,
    bus = system.lilaBus
  )

  lazy val postApi = new PostApi(
    env = this,
    indexer = hub.actor.forumSearch,
    maxPerPage = MaxPerPage(PostMaxPerPage),
    modLog = modLog,
    shutup = shutup,
    timeline = hub.actor.timeline,
    detectLanguage = detectLanguage,
    mentionNotifier = mentionNotifier,
    bus = system.lilaBus
  )

  lazy val forms = new DataForm(hub.actor.captcher)
  lazy val recent = new Recent(postApi, RecentTtl, RecentNb, asyncCache, PublicCategIds)

  system.lilaBus.subscribe(
    system.actorOf(Props(new Actor {
      def receive = {
        case CreateTeam(id, name, _) => categApi.makeTeam(id, name)
        case lila.user.User.GDPRErase(user) => postApi erase user
      }
    })),
    'team, 'gdprErase
  )

  private[forum] lazy val categColl = db(CollectionCateg)
  private[forum] lazy val topicColl = db(CollectionTopic)
  private[forum] lazy val postColl = db(CollectionPost)
}

object Env {

  lazy val current = "forum" boot new Env(
    config = lila.common.PlayApp loadConfig "forum",
    db = lila.db.Env.current,
    modLog = lila.mod.Env.current.logApi,
    shutup = lila.hub.Env.current.actor.shutup,
    hub = lila.hub.Env.current,
    detectLanguage = DetectLanguage(lila.common.PlayApp loadConfig "detectlanguage"),
    notifyApi = lila.notify.Env.current.api,
    relationApi = lila.relation.Env.current.api,
    asyncCache = lila.memo.Env.current.asyncCache,
    system = lila.common.PlayApp.system
  )
}

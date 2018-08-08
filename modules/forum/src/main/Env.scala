package lidraughts.forum

import akka.actor._
import com.typesafe.config.Config

import lidraughts.common.{ DetectLanguage, MaxPerPage }

import lidraughts.hub.actorApi.team.CreateTeam
import lidraughts.mod.ModlogApi
import lidraughts.notify.NotifyApi
import lidraughts.relation.RelationApi

final class Env(
    config: Config,
    db: lidraughts.db.Env,
    modLog: ModlogApi,
    shutup: ActorSelection,
    hub: lidraughts.hub.Env,
    detectLanguage: DetectLanguage,
    notifyApi: NotifyApi,
    relationApi: RelationApi,
    asyncCache: lidraughts.memo.AsyncCache.Builder,
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
    bus = system.lidraughtsBus
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
    bus = system.lidraughtsBus
  )

  lazy val forms = new DataForm(hub.actor.captcher)
  lazy val recent = new Recent(postApi, RecentTtl, RecentNb, asyncCache, PublicCategIds)

  system.lidraughtsBus.subscribe(
    system.actorOf(Props(new Actor {
      def receive = {
        case CreateTeam(id, name, _) => categApi.makeTeam(id, name)
      }
    })),
    'team
  )

  private[forum] lazy val categColl = db(CollectionCateg)
  private[forum] lazy val topicColl = db(CollectionTopic)
  private[forum] lazy val postColl = db(CollectionPost)
}

object Env {

  lazy val current = "forum" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "forum",
    db = lidraughts.db.Env.current,
    modLog = lidraughts.mod.Env.current.logApi,
    shutup = lidraughts.hub.Env.current.actor.shutup,
    hub = lidraughts.hub.Env.current,
    detectLanguage = DetectLanguage(lidraughts.common.PlayApp loadConfig "detectlanguage"),
    notifyApi = lidraughts.notify.Env.current.api,
    relationApi = lidraughts.relation.Env.current.api,
    asyncCache = lidraughts.memo.Env.current.asyncCache,
    system = lidraughts.common.PlayApp.system
  )
}

package lila.forum

import akka.actor._
import com.typesafe.config.Config

import lila.common.DetectLanguage
import lila.common.PimpedConfig._
import lila.hub.actorApi.forum._
import lila.mod.ModlogApi

final class Env(
    config: Config,
    db: lila.db.Env,
    modLog: ModlogApi,
    shutup: ActorSelection,
    hub: lila.hub.Env,
    detectLanguage: DetectLanguage,
    system: ActorSystem) {

  private val settings = new {
    val TopicMaxPerPage = config getInt "topic.max_per_page"
    val PostMaxPerPage = config getInt "post.max_per_page"
    val RecentTtl = config duration "recent.ttl"
    val RecentNb = config getInt "recent.nb"
    val CollectionCateg = config getString "collection.categ"
    val CollectionTopic = config getString "collection.topic"
    val CollectionPost = config getString "collection.post"
    val ActorName = config getString "actor.name"
    import scala.collection.JavaConversions._
    val PublicCategIds = (config getStringList "public_categ_ids").toList
  }
  import settings._

  lazy val categApi = new CategApi(env = this)

  lazy val topicApi = new TopicApi(
    env = this,
    indexer = hub.actor.forumSearch,
    maxPerPage = TopicMaxPerPage,
    modLog = modLog,
    shutup = shutup,
    timeline = hub.actor.timeline,
    detectLanguage = detectLanguage)

  lazy val postApi = new PostApi(
    env = this,
    indexer = hub.actor.forumSearch,
    maxPerPage = PostMaxPerPage,
    modLog = modLog,
    shutup = shutup,
    timeline = hub.actor.timeline,
    detectLanguage = detectLanguage)

  lazy val forms = new DataForm(hub.actor.captcher)
  lazy val recent = new Recent(postApi, RecentTtl, RecentNb, PublicCategIds)

  def cli = new lila.common.Cli {
    def process = {
      case "forum" :: "denormalize" :: Nil =>
        topicApi.denormalize >> categApi.denormalize inject "Forum denormalized"
    }
  }

  system.actorOf(Props(new Actor {
    def receive = {
      case MakeTeam(id, name) => categApi.makeTeam(id, name)
    }
  }), name = ActorName)

  private[forum] lazy val categColl = db(CollectionCateg)
  private[forum] lazy val topicColl = db(CollectionTopic)
  private[forum] lazy val postColl = db(CollectionPost)
}

object Env {

  private def hub = lila.hub.Env.current

  lazy val current = "forum" boot new Env(
    config = lila.common.PlayApp loadConfig "forum",
    db = lila.db.Env.current,
    modLog = lila.mod.Env.current.logApi,
    shutup = lila.hub.Env.current.actor.shutup,
    hub = lila.hub.Env.current,
    detectLanguage = DetectLanguage(lila.common.PlayApp loadConfig "detectlanguage"),
    system = lila.common.PlayApp.system)
}

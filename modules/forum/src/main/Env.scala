package lila.forum

import lila.common.PimpedConfig._
import lila.hub.actorApi.forum._
import lila.mod.ModlogApi

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env,
    modLog: ModlogApi,
    socketHub: ActorRef,
    captcher: ActorRef,
    indexer: ActorRef,
    system: ActorSystem) {

  private val settings = new {
    val TopicMaxPerPage = config getInt "topic.max_per_page"
    val PostMaxPerPage = config getInt "post.max_per_page"
    val RecentTtl = config duration "recent.ttl"
    val CollectionCateg = config getString "collection.categ"
    val CollectionTopic = config getString "collection.topic"
    val CollectionPost = config getString "collection.post"
    val ActorName = config getString "actor.name"
  }
  import settings._

  lazy val categApi = new CategApi(this)
  lazy val topicApi = new TopicApi(this, indexer, TopicMaxPerPage)
  lazy val postApi = new PostApi(this, indexer, PostMaxPerPage, modLog)

  lazy val forms = new DataForm(captcher)
  lazy val recent = new Recent(postApi, RecentTtl)

  def cli = new lila.common.Cli {
    import lila.db.api.$find
    import tube._
    def process = {
      case "forum" :: "denormalize" :: Nil ⇒
        topicApi.denormalize >> categApi.denormalize inject "Forum denormalized"
      case "forum" :: "typecheck" :: Nil ⇒
        $find.all[Categ] >> $find.all[Topic] >> $find.all[Post] inject "Forum type checked"
    }
  }

  system.actorOf(Props(new Actor {
    def receive = {
      case MakeTeam(id, name) ⇒ categApi.makeTeam(id, name)
    }
  }), name = ActorName)

  private[forum] lazy val categColl = db(CollectionCateg)
  private[forum] lazy val topicColl = db(CollectionTopic)
  private[forum] lazy val postColl = db(CollectionPost)
}

object Env {

  private def hub = lila.hub.Env.current

  lazy val current = "[boot] forum" describes new Env(
    config = lila.common.PlayApp loadConfig "forum",
    db = lila.db.Env.current,
    modLog = lila.mod.Env.current.logApi,
    socketHub = hub.socket.hub,
    captcher = hub.actor.captcher,
    indexer = hub.actor.forumIndexer,
    system = lila.common.PlayApp.system)
}

package lila.forum

import lila.common.PimpedConfig._

import akka.actor.ActorRef
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env,
    sockets: ActorRef,
    captcher: ActorRef,
    indexer: ActorRef) {

  private val settings = new {
    val TopicMaxPerPage = config getInt "topic.max_per_page"
    val PostMaxPerPage = config getInt "post.max_per_page"
    val RecentTtl = config duration "recent.ttl"
    val CollectionCateg = config getString "collection.categ"
    val CollectionTopic = config getString "collection.topic"
    val CollectionPost = config getString "collection.post"
  }
  import settings._

  lazy val categApi = new CategApi(this)
  lazy val topicApi = new TopicApi(this, indexer, TopicMaxPerPage)
  lazy val postApi = new PostApi(this, indexer, PostMaxPerPage)

  lazy val forms = new DataForm(captcher)
  lazy val recent = new Recent(postApi, RecentTtl)

  private[forum] lazy val categColl = db(CollectionCateg)
  private[forum] lazy val topicColl = db(CollectionTopic)
  private[forum] lazy val postColl = db(CollectionPost)
}

object Env {

  private def hub = lila.hub.Env.current

  lazy val current = "[forum] boot" describes new Env(
    config = lila.common.PlayApp loadConfig "forum",
    db = lila.db.Env.current,
    sockets = hub.sockets,
    captcher = hub.actor.captcher,
    indexer = hub.actor.forumIndexer)
}

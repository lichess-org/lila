package lila.forum

import lila.common.PimpedConfig._

import akka.actor.ActorRef
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env,
    sockets: ActorRef) {

  private val settings = new {
    val TopicMaxPerPage = config getInt "topic.max_per_page"
    val PostMaxPerPage = config getInt "post.max_per_page"
    val SearchMaxPerPage = config getInt "search.max_per_page"
    val RecentTimeout = config millis "recent.timeout"
    val CollectionCateg = config getString "collection.categ"
    val CollectionTopic = config getString "collection.topic"
    val CollectionPost = config getString "collection.post"
  }
  import settings._

  lazy val categApi = new CategApi(this)

  private[forum] lazy val categColl = db(CollectionCateg)

  private[forum] lazy val topicColl = db(CollectionTopic)

  private[forum] lazy val postColl = db(CollectionPost)
}

object Env {

  lazy val current = "[forum] boot" describes new Env(
    config = lila.common.PlayApp loadConfig "forum",
    db = lila.db.Env.current,
    sockets = lila.hub.Env.current.sockets)
}

package lila.video

import akka.actor.{ ActorSelection, ActorSystem }
import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env) {

  private val settings = new {
    val CollectionVideo = config getString "collection.video"
    val CollectionView = config getString "collection.view"
  }
  import settings._

  lazy val api = new VideoApi(
    videoColl = videoColl,
    viewColl = viewColl)

  private[video] lazy val videoColl = db(CollectionVideo)
  private[video] lazy val viewColl = db(CollectionView)
}

object Env {

  lazy val current: Env = "[boot] opening" describes new Env(
    config = lila.common.PlayApp loadConfig "opening",
    db = lila.db.Env.current)
}

package lila.video

import akka.actor.{ ActorSelection, ActorSystem }
import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    scheduler: lila.common.Scheduler,
    db: lila.db.Env) {

  private val settings = new {
    val CollectionVideo = config getString "collection.video"
    val CollectionView = config getString "collection.view"
    val SheetUrl = config getString "sheet.url"
    val SheetDelay = config duration "sheet.delay"
  }
  import settings._

  lazy val api = new VideoApi(
    videoColl = videoColl,
    viewColl = viewColl)

  private lazy val fetch = new FetchSheet(
    url = SheetUrl,
    api = api)

  scheduler.effect(SheetDelay, "video update") {
    fetch.apply
  }

  private[video] lazy val videoColl = db(CollectionVideo)
  private[video] lazy val viewColl = db(CollectionView)
}

object Env {

  lazy val current: Env = "[boot] video" describes new Env(
    config = lila.common.PlayApp loadConfig "video",
    scheduler = lila.common.PlayApp.scheduler,
    db = lila.db.Env.current)
}

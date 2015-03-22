package lila.video

import akka.actor.{ ActorSelection, ActorSystem }
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    scheduler: lila.common.Scheduler,
    db: lila.db.Env) {

  private val settings = new {
    val CollectionVideo = config getString "collection.video"
    val CollectionView = config getString "collection.view"
    val CollectionFilter = config getString "collection.filter"
    val SheetUrl = config getString "sheet.url"
    val SheetDelay = config duration "sheet.delay"
    val YoutubeUrl = config getString "youtube.url"
    val YoutubeApiKey = config getString "youtube.api_key"
    val YoutubeMax = config getInt "youtube.max"
    val YoutubeDelay = config duration "youtube.delay"
  }
  import settings._

  lazy val api = new VideoApi(
    videoColl = videoColl,
    viewColl = viewColl,
  filterColl = filterColl)

  private lazy val sheet = new Sheet(
    url = SheetUrl,
    api = api)

  private lazy val youtube = new Youtube(
    url = YoutubeUrl,
    apiKey = YoutubeApiKey,
    max = YoutubeMax,
    api = api)

  scheduler.effect(SheetDelay, "video update from sheet") {
    sheet.fetchAll
  }

  scheduler.effect(YoutubeDelay, "video update from youtube") {
    youtube.updateAll
  }

  scheduler.once(5 seconds) {
    sheet.fetchAll >> youtube.updateAll logFailure "video boot"
  }

  private[video] lazy val videoColl = db(CollectionVideo)
  private[video] lazy val viewColl = db(CollectionView)
  private[video] lazy val filterColl = db(CollectionFilter)
}

object Env {

  lazy val current: Env = "[boot] video" describes new Env(
    config = lila.common.PlayApp loadConfig "video",
    scheduler = lila.common.PlayApp.scheduler,
    db = lila.db.Env.current)
}

package lila.video

import akka.actor.{ ActorSelection, ActorSystem }
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    scheduler: lila.common.Scheduler,
    db: lila.db.Env,
    isDev: Boolean) {

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

  private lazy val paginator = new VideoPaginator(
    videoColl = videoColl,
    viewColl = viewColl)

  lazy val api = new VideoApi(
    videoColl = videoColl,
    viewColl = viewColl,
    filterColl = filterColl,
    paginator = paginator)

  private lazy val sheet = new Sheet(
    url = SheetUrl,
    api = api)

  private lazy val youtube = new Youtube(
    url = YoutubeUrl,
    apiKey = YoutubeApiKey,
    max = YoutubeMax,
    api = api)

  if (!isDev) {
    scheduler.effect(SheetDelay, "video update from sheet") {
      sheet.fetchAll logFailure "video sheet"
    }

    scheduler.effect(YoutubeDelay, "video update from youtube") {
      youtube.updateAll logFailure "video youtube"
    }
  }

  private[video] lazy val videoColl = db(CollectionVideo)
  private[video] lazy val viewColl = db(CollectionView)
  private[video] lazy val filterColl = db(CollectionFilter)
}

object Env {

  lazy val current: Env = "[boot] video" describes new Env(
    config = lila.common.PlayApp loadConfig "video",
    scheduler = lila.common.PlayApp.scheduler,
    isDev = lila.common.PlayApp.isDev,
    db = lila.db.Env.current)
}

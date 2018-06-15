package lila.video

import com.typesafe.config.Config

final class Env(
    config: Config,
    scheduler: lila.common.Scheduler,
    db: lila.db.Env,
    asyncCache: lila.memo.AsyncCache.Builder,
    isDev: Boolean
) {

  private val settings = new {
    val CollectionVideo = config getString "collection.video"
    val CollectionView = config getString "collection.view"
    val SheetUrl = config getString "sheet.url"
    val SheetDelay = config duration "sheet.delay"
    val YoutubeUrl = config getString "youtube.url"
    val YoutubeApiKey = config getString "youtube.api_key"
    val YoutubeMax = config getInt "youtube.max"
    val YoutubeDelay = config duration "youtube.delay"
  }
  import settings._

  lazy val api = new VideoApi(
    asyncCache = asyncCache,
    videoColl = videoColl,
    viewColl = viewColl
  )

  private lazy val sheet = new Sheet(
    url = SheetUrl,
    api = api
  )

  private lazy val youtube = new Youtube(
    url = YoutubeUrl,
    apiKey = YoutubeApiKey,
    max = YoutubeMax,
    api = api
  )

  if (!isDev) {
    scheduler.effect(SheetDelay, "video update from sheet") {
      sheet.fetchAll logFailure logger nevermind
    }

    scheduler.effect(YoutubeDelay, "video update from youtube") {
      youtube.updateAll logFailure logger nevermind
    }
  }

  private[video] lazy val videoColl = db(CollectionVideo)
  private[video] lazy val viewColl = db(CollectionView)
}

object Env {

  lazy val current: Env = "video" boot new Env(
    config = lila.common.PlayApp loadConfig "video",
    scheduler = lila.common.PlayApp.scheduler,
    isDev = lila.common.PlayApp.isDev,
    asyncCache = lila.memo.Env.current.asyncCache,
    db = lila.db.Env.current
  )
}

package lila.video

import play.api.libs.ws.WSClient
import play.api.{ Mode, Configuration }
import scala.concurrent.duration._

import lila.common.CollName
import lila.common.config._

final class Env(
    appConfig: Configuration,
    scheduler: akka.actor.Scheduler,
    db: lila.db.Env,
    asyncCache: lila.memo.AsyncCache.Builder,
    ws: WSClient,
    mode: Mode
) {

  private val config = appConfig.get[Configuration]("video")
  private val CollectionVideo = config.get[CollName]("collection.video")
  private val CollectionView = config.get[CollName]("collection.view")
  private val SheetUrl = config.get[String]("sheet.url")
  private val SheetDelay = config.get[FiniteDuration]("sheet.delay")
  private val YoutubeUrl = config.get[String]("youtube.url")
  private val YoutubeApiKey = config.get[String]("youtube.api_key")
  private val YoutubeMax = config.get[Int]("youtube.max")
  private val YoutubeDelay = config.get[FiniteDuration]("youtube.delay")

  private lazy val videoColl = db(CollectionVideo)
  private lazy val viewColl = db(CollectionView)

  lazy val api = new VideoApi(
    asyncCache = asyncCache,
    videoColl = videoColl,
    viewColl = viewColl
  )

  private lazy val sheet = new Sheet(ws, SheetUrl, api)

  private lazy val youtube = new Youtube(
    ws = ws,
    url = YoutubeUrl,
    apiKey = YoutubeApiKey,
    max = YoutubeMax,
    api = api
  )

  if (mode == Mode.Prod) {
    scheduler.scheduleWithFixedDelay(SheetDelay, SheetDelay) { () =>
      sheet.fetchAll logFailure logger nevermind
    }

    scheduler.scheduleWithFixedDelay(YoutubeDelay, YoutubeDelay) { () =>
      youtube.updateAll logFailure logger nevermind
    }
  }
}

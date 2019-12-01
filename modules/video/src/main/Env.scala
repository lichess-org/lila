package lila.video

import play.api.libs.ws.WSClient
import play.api.{ Mode, Configuration }
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import scala.concurrent.duration._

import lila.common.config._

@Module
private class VideoConfig(
    @ConfigName("collection.video") val videoColl: CollName,
    @ConfigName("collection.view") val viewColl: CollName,
    @ConfigName("sheet.url") val sheetUrl: String,
    @ConfigName("sheet.delay") val sheetDelay: FiniteDuration,
    @ConfigName("youtube.url") val youtubeUrl: String,
    @ConfigName("youtube.api_key") val youtubeApiKey: Secret,
    @ConfigName("youtube.max") val youtubeMax: Max,
    @ConfigName("youtube.delay") val youtubeDelay: FiniteDuration
)

final class Env(
    appConfig: Configuration,
    ws: WSClient,
    scheduler: akka.actor.Scheduler,
    db: lila.db.Env,
    asyncCache: lila.memo.AsyncCache.Builder,
    mode: Mode
)(implicit system: akka.actor.ActorSystem) {

  private val config = appConfig.get[VideoConfig]("video")(AutoConfig.loader)

  lazy val api = new VideoApi(
    asyncCache = asyncCache,
    videoColl = db(config.videoColl),
    viewColl = db(config.viewColl)
  )

  private lazy val sheet = new Sheet(ws, config.sheetUrl, api)

  private lazy val youtube = new Youtube(
    ws = ws,
    url = config.youtubeUrl,
    apiKey = config.youtubeApiKey,
    max = config.youtubeMax,
    api = api
  )

  if (mode == Mode.Prod) {
    scheduler.scheduleWithFixedDelay(config.sheetDelay * 2, config.sheetDelay) { () =>
      sheet.fetchAll logFailure logger nevermind
    }

    scheduler.scheduleWithFixedDelay(config.youtubeDelay * 2, config.youtubeDelay) { () =>
      youtube.updateAll logFailure logger nevermind
    }
  }
}

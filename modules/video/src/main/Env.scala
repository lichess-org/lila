package lila.video

import com.softwaremill.macwire.*
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient

import lila.common.autoconfig.{ *, given }
import lila.common.config.given
import lila.core.config.*

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
    ws: StandaloneWSClient,
    scheduler: Scheduler,
    db: lila.db.Db,
    cacheApi: lila.memo.CacheApi,
    mode: play.api.Mode
)(using Executor, Scheduler):

  private val config = appConfig.get[VideoConfig]("video")(using AutoConfig.loader)

  lazy val api = VideoApi(
    cacheApi = cacheApi,
    videoColl = db(config.videoColl),
    viewColl = db(config.viewColl)
  )

  private lazy val sheet = VideoSheet(ws, config.sheetUrl, api)

  private lazy val youtube = Youtube(
    ws = ws,
    url = config.youtubeUrl,
    apiKey = config.youtubeApiKey,
    max = config.youtubeMax,
    api = api
  )

  def cli: lila.common.Cli = new:
    def process =
      case "video" :: "sheet" :: Nil =>
        sheet.fetchAll.map { nb => s"Processed $nb videos" }

  if mode.isProd then
    scheduler.scheduleWithFixedDelay(config.sheetDelay, config.sheetDelay): () =>
      sheet.fetchAll.logFailure(logger)

    scheduler.scheduleWithFixedDelay(config.youtubeDelay, config.youtubeDelay): () =>
      youtube.updateAll.logFailure(logger)

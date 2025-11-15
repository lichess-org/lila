package lila.jsBot

import com.softwaremill.macwire.*

import lila.core.config.*
import lila.common.config.GetRelativeFile

@Module
final class Env(
    db: lila.db.Db,
    getFile: GetRelativeFile,
    cacheApi: lila.memo.CacheApi
)(using Executor, Scheduler, akka.stream.Materializer):

  val repo = JsBotRepo(db(CollName("jsbot")), db(CollName("jsbot_asset")))

  val api: JsBotApi = wire[JsBotApi]

  val assets: JsBotAssetApi = wire[JsBotAssetApi]

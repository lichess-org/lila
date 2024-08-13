package lila.local

import com.softwaremill.macwire.*
import play.api.libs.ws.StandaloneWSClient

import lila.core.config.CollName
import lila.memo.CacheApi

@Module
final class Env(db: lila.db.Db, cacheApi: CacheApi, getFile: (String => java.io.File))(using
    Executor,
    akka.stream.Materializer
)(using mode: play.api.Mode, scheduler: Scheduler):

  val repo = LocalRepo(db(CollName("local_bots")), db(CollName("local_assets")))
  val api  = LocalApi(repo, getFile)

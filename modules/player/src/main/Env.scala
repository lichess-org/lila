package lila.player

import com.softwaremill.macwire.*
import play.api.libs.ws.StandaloneWSClient

import lila.db.dsl.Coll
import lila.memo.CacheApi
import lila.common.config.CollName

@Module
final class Env(db: lila.db.Db, cacheApi: CacheApi, ws: StandaloneWSClient)(using
    Executor,
    Scheduler,
    akka.stream.Materializer
):
  private val fidePlayerColl = db(CollName("fide_player"))

  private val fideSync = wire[FidePlayerSync]

  lazy val fideApi = wire[FidePlayerApi]

  def cli = new lila.common.Cli:
    def process =
      case "fide" :: "player" :: "sync" :: Nil =>
        fideSync()
        fuccess("Updating the player database in the background.")

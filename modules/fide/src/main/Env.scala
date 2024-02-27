package lila.fide

import com.softwaremill.macwire.*
import play.api.libs.ws.StandaloneWSClient
import play.api.Mode

import lila.db.dsl.Coll
import lila.memo.CacheApi
import lila.common.config.CollName

@Module
final class Env(db: lila.db.Db, cacheApi: CacheApi, ws: StandaloneWSClient)(using
    Executor,
    akka.stream.Materializer
)(using mode: Mode, scheduler: Scheduler):

  private val fidePlayerColl = db(CollName("fide_player"))

  lazy val api = wire[FidePlayerApi]

  lazy val paginator = wire[FidePaginator]

  private lazy val fideSync = wire[FidePlayerSync]

  if mode == Mode.Prod then
    scheduler.scheduleWithFixedDelay(1.hour, 1.hour): () =>
      if nowDateTime.getDayOfWeek == java.time.DayOfWeek.SUNDAY && nowDateTime.getHour == 4
      then fideSync()

  def cli = new lila.common.Cli:
    def process =
      case "fide" :: "player" :: "sync" :: Nil =>
        fideSync()
        fuccess("Updating the player database in the background.")

package lila.racer

import com.softwaremill.macwire.*

import lila.core.LightUser
import lila.db.AsyncColl
import lila.storm.{ StormJson, StormSelector, StormSign }

@Module
final class Env(
    selector: StormSelector,
    socketKit: lila.core.socket.SocketKit,
    puzzleColls: lila.puzzle.PuzzleColls,
    cacheApi: lila.memo.CacheApi,
    stormJson: StormJson,
    stormSign: StormSign,
    userApi: lila.core.user.UserApi,
    lightUserGetter: LightUser.GetterSyncFallback,
    db: lila.db.Db
)(using Executor, Scheduler, play.api.Mode):

  lazy val api = wire[RacerApi]

  lazy val lobby = wire[RacerLobby]

  lazy val json = wire[RacerJson]

  wire[RacerSocket] // requires eager eval

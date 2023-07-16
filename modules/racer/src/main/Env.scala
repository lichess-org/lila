package lila.racer

import com.softwaremill.macwire.*

import lila.common.LightUser
import lila.db.AsyncColl
import lila.storm.{ StormJson, StormSelector, StormSign }

@Module
@annotation.nowarn("msg=unused")
final class Env(
    selector: StormSelector,
    puzzleColls: lila.puzzle.PuzzleColls,
    cacheApi: lila.memo.CacheApi,
    stormJson: StormJson,
    stormSign: StormSign,
    userRepo: lila.user.UserRepo,
    perfsRepo: lila.user.UserPerfsRepo,
    lightUserGetter: LightUser.GetterSyncFallback,
    remoteSocketApi: lila.socket.RemoteSocket,
    db: lila.db.Db
)(using Executor, Scheduler, play.api.Mode):

  private lazy val colls = RacerColls(puzzle = puzzleColls.puzzle)

  lazy val api = wire[RacerApi]

  lazy val lobby = wire[RacerLobby]

  lazy val json = wire[RacerJson]

  wire[RacerSocket] // requires eager eval

final private class RacerColls(val puzzle: AsyncColl)

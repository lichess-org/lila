package lila.racer

import com.softwaremill.macwire.*

import lila.common.LightUser
import lila.db.AsyncColl
import lila.storm.StormJson
import lila.storm.StormSelector
import lila.storm.StormSign

@Module
final class Env(
    selector: StormSelector,
    puzzleColls: lila.puzzle.PuzzleColls,
    cacheApi: lila.memo.CacheApi,
    stormJson: StormJson,
    stormSign: StormSign,
    userRepo: lila.user.UserRepo,
    lightUserGetter: LightUser.GetterSync,
    remoteSocketApi: lila.socket.RemoteSocket,
    db: lila.db.Db
)(using
    ec: scala.concurrent.ExecutionContext,
    scheduler: akka.actor.Scheduler,
    mode: play.api.Mode
):

  private lazy val colls = new RacerColls(puzzle = puzzleColls.puzzle)

  lazy val api = wire[RacerApi]

  lazy val lobby = wire[RacerLobby]

  lazy val json = wire[RacerJson]

  private val socket = wire[RacerSocket] // requires eager eval

final private class RacerColls(val puzzle: AsyncColl)

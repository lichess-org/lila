package lila.racer

import com.softwaremill.macwire._
import play.api.Configuration

import lila.common.config._
import lila.common.LightUser
import lila.db.AsyncColl
import lila.db.dsl.Coll
import lila.storm.StormJson
import lila.storm.StormSelector
import lila.storm.StormSign
import lila.user.UserRepo

@Module
final class Env(
    selector: StormSelector,
    puzzleColls: lila.puzzle.PuzzleColls,
    cacheApi: lila.memo.CacheApi,
    stormJson: StormJson,
    stormSign: StormSign,
    lightUserGetter: LightUser.GetterSync,
    db: lila.db.Db
)(implicit
    ec: scala.concurrent.ExecutionContext
) {

  private lazy val colls = new RacerColls(puzzle = puzzleColls.puzzle)

  lazy val api = wire[RacerApi]

  lazy val json = wire[RacerJson]
}

final private class RacerColls(val puzzle: AsyncColl)

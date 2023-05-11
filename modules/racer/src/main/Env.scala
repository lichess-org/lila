package lila.racer

import com.softwaremill.macwire.*

import lila.common.LightUser
import lila.db.AsyncColl
import lila.storm.StormSelector

@Module
final class Env(
    selector: StormSelector,
    cacheApi: lila.memo.CacheApi,
    userRepo: lila.user.UserRepo,
    lightUserGetter: LightUser.GetterSyncFallback
)(using Executor, Scheduler, play.api.Mode):

  lazy val api = wire[RacerApi]

  lazy val lobby = wire[RacerLobby]

  lazy val json = wire[RacerJson]

final private class RacerColls(val puzzle: AsyncColl)

package lila.racer

import com.softwaremill.macwire.*

import lila.core.LightUser
import lila.storm.StormSelector

@Module
final class Env(
    selector: StormSelector,
    socketKit: lila.core.socket.SocketKit,
    cacheApi: lila.memo.CacheApi,
    userApi: lila.core.user.UserApi,
    lightUserGetter: LightUser.GetterSyncFallback
)(using Executor, Scheduler, play.api.Mode):

  lazy val api = wire[RacerApi]

  lazy val lobby = wire[RacerLobby]

  lazy val json = wire[RacerJson]

  wire[RacerSocket] // requires eager eval

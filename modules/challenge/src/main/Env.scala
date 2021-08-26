package lila.challenge

import com.softwaremill.macwire._
import play.api.Configuration
import scala.concurrent.duration._

import lila.common.config._
import lila.socket.Socket.{ GetVersion, SocketVersion }

@Module
final class Env(
    appConfig: Configuration,
    gameRepo: lila.game.GameRepo,
    userRepo: lila.user.UserRepo,
    onStart: lila.round.OnStart,
    gameCache: lila.game.Cached,
    lightUser: lila.common.LightUser.GetterSync,
    lightUserApi: lila.user.LightUserApi,
    isOnline: lila.socket.IsOnline,
    db: lila.db.Db,
    cacheApi: lila.memo.CacheApi,
    prefApi: lila.pref.PrefApi,
    relationApi: lila.relation.RelationApi,
    remoteSocketApi: lila.socket.RemoteSocket,
    msgApi: lila.msg.MsgApi,
    baseUrl: BaseUrl
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem,
    scheduler: akka.actor.Scheduler,
    mode: play.api.Mode
) {

  private lazy val maxPlaying = appConfig.get[Max]("setup.max_playing")

  private val colls = wire[ChallengeColls]

  def version(challengeId: Challenge.ID): Fu[SocketVersion] =
    socket.rooms.ask[SocketVersion](challengeId)(GetVersion)

  private lazy val joiner = wire[ChallengeJoiner]

  lazy val maker = wire[ChallengeMaker]

  lazy val api = wire[ChallengeApi]

  private lazy val socket = wire[ChallengeSocket]

  lazy val granter = wire[ChallengeGranter]

  private lazy val repo = wire[ChallengeRepo]

  lazy val jsonView = wire[JsonView]

  lazy val bulk = wire[ChallengeBulkApi]

  lazy val msg = wire[ChallengeMsg]

  lazy val keepAliveStream = wire[ChallengeKeepAliveStream]

  val forms = new ChallengeForm

  system.scheduler.scheduleWithFixedDelay(10 seconds, 3343 millis) { () =>
    api.sweep.unit
  }

  system.scheduler.scheduleWithFixedDelay(20 seconds, 2897 millis) { () =>
    bulk.tick.unit
  }
}

private class ChallengeColls(db: lila.db.Db) {
  val challenge = db(CollName("challenge"))
  val bulk      = db(CollName("challenge_bulk"))
}

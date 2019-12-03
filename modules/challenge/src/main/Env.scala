package lila.challenge

import akka.actor._
import com.softwaremill.macwire._
import play.api.Configuration
import scala.concurrent.duration._

import lila.common.config._
import lila.game.Game
import lila.socket.Socket.{ SocketVersion, GetVersion }
import lila.user.User

final class Env(
    appConfig: Configuration,
    gameRepo: lila.game.GameRepo,
    userRepo: lila.user.UserRepo,
    onStart: lila.round.OnStart,
    gameCache: lila.game.Cached,
    lightUser: lila.common.LightUser.GetterSync,
    isOnline: lila.user.IsOnline,
    db: lila.db.Env,
    asyncCache: lila.memo.AsyncCache.Builder,
    prefApi: lila.pref.PrefApi,
    relationApi: lila.relation.RelationApi,
    remoteSocketApi: lila.socket.RemoteSocket
)(implicit system: akka.actor.ActorSystem) {

  private lazy val maxPlaying = appConfig.get[Max]("setup.max_playing")

  def version(challengeId: Challenge.ID): Fu[SocketVersion] =
    socket.rooms.ask[SocketVersion](challengeId)(GetVersion)

  private lazy val joiner = wire[Joiner]

  private lazy val maker = wire[ChallengeMaker]

  lazy val api = wire[ChallengeApi]

  private lazy val socket = wire[ChallengeSocket]

  lazy val granter = wire[ChallengeGranter]

  private lazy val repo = new ChallengeRepo(
    coll = db(CollName("challenge")),
    maxPerUser = maxPlaying
  )

  lazy val jsonView = wire[JsonView]

  system.scheduler.scheduleWithFixedDelay(10 seconds, 3 seconds) {
    () => api.sweep
  }
}

package lila.challenge

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.game.Game
import lila.socket.Socket.{ SocketVersion, GetVersion }
import lila.user.User

final class Env(
    config: Config,
    system: ActorSystem,
    onStart: Game.ID => Unit,
    gameCache: lila.game.Cached,
    lightUser: lila.common.LightUser.GetterSync,
    isOnline: lila.user.User.ID => Boolean,
    hub: lila.hub.Env,
    db: lila.db.Env,
    asyncCache: lila.memo.AsyncCache.Builder,
    getPref: User => Fu[lila.pref.Pref],
    getRelation: (User, User) => Fu[Option[lila.relation.Relation]],
    remoteSocketApi: lila.socket.RemoteSocket,
    scheduler: lila.common.Scheduler
) {

  private val settings = new {
    val CollectionChallenge = config getString "collection.challenge"
    val MaxPerUser = config getInt "max_per_user"
    val MaxPlaying = config getInt "max_playing"
  }
  import settings._

  def version(challengeId: Challenge.ID): Fu[SocketVersion] =
    socket.rooms.ask[SocketVersion](challengeId)(GetVersion)

  lazy val api = new ChallengeApi(
    repo = repo,
    joiner = new Joiner(onStart = onStart),
    jsonView = jsonView,
    gameCache = gameCache,
    maxPlaying = MaxPlaying,
    asyncCache = asyncCache,
    lilaBus = system.lilaBus
  )

  private lazy val socket = new ChallengeSocket(
    api = api,
    remoteSocketApi = remoteSocketApi
  )

  lazy val granter = new ChallengeGranter(
    getPref = getPref,
    getRelation = getRelation
  )

  private lazy val repo = new ChallengeRepo(
    coll = db(CollectionChallenge),
    maxPerUser = MaxPerUser
  )

  lazy val jsonView = new JsonView(lightUser, isOnline)

  scheduler.future(3 seconds, "sweep challenges") {
    api.sweep
  }
}

object Env {

  lazy val current: Env = "challenge" boot new Env(
    config = lila.common.PlayApp loadConfig "challenge",
    system = lila.common.PlayApp.system,
    onStart = lila.round.Env.current.onStart,
    hub = lila.hub.Env.current,
    gameCache = lila.game.Env.current.cached,
    lightUser = lila.user.Env.current.lightUserSync,
    isOnline = lila.user.Env.current.isOnline,
    db = lila.db.Env.current,
    asyncCache = lila.memo.Env.current.asyncCache,
    getPref = lila.pref.Env.current.api.getPref,
    getRelation = lila.relation.Env.current.api.fetchRelation,
    remoteSocketApi = lila.socket.Env.current.remoteSocket,
    scheduler = lila.common.PlayApp.scheduler
  )
}

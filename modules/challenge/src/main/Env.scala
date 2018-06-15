package lila.challenge

import akka.actor._
import akka.pattern.ask
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.user.User
import lila.hub.actorApi.map.Ask
import lila.socket.actorApi.GetVersion
import makeTimeout.short

final class Env(
    config: Config,
    system: ActorSystem,
    onStart: String => Unit,
    gameCache: lila.game.Cached,
    lightUser: lila.common.LightUser.GetterSync,
    isOnline: lila.user.User.ID => Boolean,
    hub: lila.hub.Env,
    db: lila.db.Env,
    asyncCache: lila.memo.AsyncCache.Builder,
    getPref: User => Fu[lila.pref.Pref],
    getRelation: (User, User) => Fu[Option[lila.relation.Relation]],
    scheduler: lila.common.Scheduler
) {

  private val settings = new {
    val CollectionChallenge = config getString "collection.challenge"
    val MaxPerUser = config getInt "max_per_user"
    val HistoryMessageTtl = config duration "history.message.ttl"
    val UidTimeout = config duration "uid.timeout"
    val SocketTimeout = config duration "socket.timeout"
    val SocketName = config getString "socket.name"
    val MaxPlaying = config getInt "max_playing"
  }
  import settings._

  private val socketHub = system.actorOf(
    Props(new lila.socket.SocketHubActor.Default[Socket] {
      def mkActor(challengeId: String) = new Socket(
        challengeId = challengeId,
        history = new lila.socket.History(ttl = HistoryMessageTtl),
        getChallenge = repo.byId,
        uidTimeout = UidTimeout,
        socketTimeout = SocketTimeout
      )
    }), name = SocketName
  )

  def version(challengeId: Challenge.ID): Fu[Int] =
    socketHub ? Ask(challengeId, GetVersion) mapTo manifest[Int]

  lazy val socketHandler = new SocketHandler(
    hub = hub,
    socketHub = socketHub,
    pingChallenge = api.ping
  )

  lazy val api = new ChallengeApi(
    repo = repo,
    joiner = new Joiner(onStart = onStart),
    jsonView = jsonView,
    gameCache = gameCache,
    maxPlaying = MaxPlaying,
    socketHub = socketHub,
    userRegister = hub.actor.userRegister,
    asyncCache = asyncCache,
    lilaBus = system.lilaBus
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
    onStart = lila.game.Env.current.onStart,
    hub = lila.hub.Env.current,
    gameCache = lila.game.Env.current.cached,
    lightUser = lila.user.Env.current.lightUserSync,
    isOnline = lila.user.Env.current.isOnline,
    db = lila.db.Env.current,
    asyncCache = lila.memo.Env.current.asyncCache,
    getPref = lila.pref.Env.current.api.getPref,
    getRelation = lila.relation.Env.current.api.fetchRelation,
    scheduler = lila.common.PlayApp.scheduler
  )
}

package lila.challenge

import com.softwaremill.macwire.*

import lila.common.config.*
import lila.hub.socket.{ GetVersion, SocketVersion }

@Module
final class Env(
    gameRepo: lila.game.GameRepo,
    userRepo: lila.user.UserRepo,
    perfsRepo: lila.user.UserPerfsRepo,
    userApi: lila.user.UserApi,
    onStart: lila.round.OnStart,
    gameCache: lila.game.Cached,
    rematches: lila.game.Rematches,
    lightUser: lila.common.LightUser.GetterSync,
    lightUserApi: lila.user.LightUserApi,
    isOnline: lila.hub.socket.IsOnline,
    db: lila.db.Db,
    cacheApi: lila.memo.CacheApi,
    prefApi: lila.pref.PrefApi,
    relationApi: lila.relation.RelationApi,
    socketKit: lila.hub.socket.SocketKit,
    getLagRating: lila.hub.socket.userLag.GetLagRating,
    msgApi: lila.msg.MsgApi,
    langPicker: lila.hub.i18n.LangPicker,
    jsDump: lila.hub.i18n.JsDump,
    setupForm: lila.hub.setup.SetupForm,
    oauthServer: lila.oauth.OAuthServer,
    baseUrl: BaseUrl
)(using
    scheduler: Scheduler
)(using
    akka.actor.ActorSystem,
    Executor,
    akka.stream.Materializer,
    lila.game.IdGenerator,
    play.api.Mode,
    lila.hub.i18n.Translator
):

  private val colls = wire[ChallengeColls]

  def version(challengeId: Challenge.Id): Fu[SocketVersion] =
    socket.rooms.ask[SocketVersion](challengeId.into(RoomId))(GetVersion.apply)

  private lazy val joiner = wire[ChallengeJoiner]

  lazy val maker = wire[ChallengeMaker]

  lazy val api = wire[ChallengeApi]

  private lazy val socket = wire[ChallengeSocket]

  lazy val granter = wire[ChallengeGranter]

  private lazy val repo = wire[ChallengeRepo]

  lazy val jsonView = wire[JsonView]

  lazy val bulkSetup = wire[ChallengeBulkSetup]

  lazy val bulkSetupApi = wire[ChallengeBulkSetupApi]

  lazy val bulk = wire[ChallengeBulkApi]

  lazy val msg = wire[ChallengeMsg]

  lazy val keepAliveStream = wire[ChallengeKeepAliveStream]

  val forms = new ChallengeForm

  scheduler.scheduleWithFixedDelay(10 seconds, 3343 millis): () =>
    api.sweep

  scheduler.scheduleWithFixedDelay(20 seconds, 2897 millis): () =>
    bulk.tick

  lila.common.Bus.subscribeFun("roundUnplayed"):
    case lila.hub.actorApi.round.DeleteUnplayed(gameId) => api.removeByGameId(gameId)

private class ChallengeColls(db: lila.db.Db):
  val challenge = db(CollName("challenge"))
  val bulk      = db(CollName("challenge_bulk"))

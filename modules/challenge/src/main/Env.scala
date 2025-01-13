package lila.challenge

import com.softwaremill.macwire.*

import lila.core.config.*
import lila.core.socket.{ GetVersion, SocketVersion }

@Module
final class Env(
    gameRepo: lila.game.GameRepo,
    userApi: lila.core.user.UserApi,
    onStart: lila.core.game.OnStart,
    gameCache: lila.game.Cached,
    rematches: lila.game.Rematches,
    lightUser: lila.core.LightUser.GetterSync,
    lightUserApi: lila.core.user.LightUserApi,
    isOnline: lila.core.socket.IsOnline,
    db: lila.db.Db,
    cacheApi: lila.memo.CacheApi,
    prefApi: lila.core.pref.PrefApi,
    relationApi: lila.core.relation.RelationApi,
    socketKit: lila.core.socket.SocketKit,
    getLagRating: lila.core.socket.userLag.GetLagRating,
    msgApi: lila.core.msg.MsgApi,
    langPicker: lila.core.i18n.LangPicker,
    setupForm: lila.core.setup.SetupForm,
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
    lila.core.i18n.Translator,
    lila.core.config.RateLimit
):

  private val colls = wire[ChallengeColls]

  def version(challengeId: ChallengeId): Fu[SocketVersion] =
    socket.rooms.ask[SocketVersion](challengeId.into(RoomId))(GetVersion.apply)

  private lazy val joiner = wire[ChallengeJoiner]

  lazy val maker = wire[ChallengeMaker]

  lazy val api = wire[ChallengeApi]

  private val socket = wire[ChallengeSocket]

  lazy val granter = wire[ChallengeGranter]

  private lazy val repo = wire[ChallengeRepo]

  lazy val jsonView = wire[JsonView]

  lazy val bulkSetup = wire[ChallengeBulkSetup]

  lazy val bulkSetupApi = wire[ChallengeBulkSetupApi]

  lazy val bulk = wire[ChallengeBulkApi]

  lazy val msg = wire[ChallengeMsg]

  lazy val keepAliveStream = wire[ChallengeKeepAliveStream]

  val forms = new ChallengeForm

  scheduler.scheduleWithFixedDelay(10.seconds, 3343.millis): () =>
    api.sweep

  scheduler.scheduleWithFixedDelay(20.seconds, 2897.millis): () =>
    bulk.tick

  lila.common.Bus.subscribeFun("roundUnplayed"):
    case lila.core.round.DeleteUnplayed(gameId) => api.removeByGameId(gameId)

private class ChallengeColls(db: lila.db.Db):
  val challenge = db(CollName("challenge"))
  val bulk      = db(CollName("challenge_bulk"))

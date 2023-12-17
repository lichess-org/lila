package lila.push

import akka.actor.*
import com.softwaremill.macwire.*
import lila.common.autoconfig.{ *, given }
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient

import lila.common.config.*

@Module
final private class PushConfig(
    @ConfigName("collection.device") val deviceColl: CollName,
    @ConfigName("collection.subscription") val subscriptionColl: CollName,
    val web: WebPush.Config,
    val firebase: FirebasePush.BothConfigs
)

@Module
final class Env(
    appConfig: Configuration,
    ws: StandaloneWSClient,
    db: lila.db.Db,
    getLightUser: lila.common.LightUser.GetterFallback,
    proxyRepo: lila.round.GameProxyRepo,
    gameRepo: lila.game.GameRepo,
    notifyAllows: lila.notify.GetNotifyAllows,
    postApi: lila.forum.ForumPostApi
)(using Executor, Scheduler):

  private val config = appConfig.get[PushConfig]("push")(AutoConfig.loader)

  def vapidPublicKey = config.web.vapidPublicKey

  private val deviceApi  = DeviceApi(db(config.deviceColl))
  val webSubscriptionApi = WebSubscriptionApi(db(config.subscriptionColl))

  export deviceApi.{ register as registerDevice, unregister as unregisterDevices }

  private lazy val firebasePush = wire[FirebasePush]

  private lazy val webPush = wire[WebPush]

  private lazy val pushApi: PushApi = wire[PushApi]

  private def logUnit(f: Fu[?]): Unit =
    f logFailure logger
    ()
  lila.common.Bus.subscribeFun(
    "finishGame",
    "moveEventCorres",
    "challenge",
    "corresAlarm",
    "offerEventCorres",
    "tourSoon",
    "notifyPush"
  ):
    case lila.game.actorApi.FinishGame(game, _) =>
      logUnit { pushApi finish game }
    case lila.hub.actorApi.round.CorresMoveEvent(move, _, pushable, _, _) if pushable =>
      logUnit { pushApi move move }
    case lila.hub.actorApi.round.CorresTakebackOfferEvent(gameId) =>
      logUnit { pushApi takebackOffer gameId }
    case lila.hub.actorApi.round.CorresDrawOfferEvent(gameId) =>
      logUnit { pushApi drawOffer gameId }
    case lila.challenge.Event.Create(c) =>
      logUnit { pushApi challengeCreate c }
    case lila.challenge.Event.Accept(c, joinerId) =>
      logUnit { pushApi.challengeAccept(c, joinerId) }
    case lila.game.actorApi.CorresAlarmEvent(pov) =>
      logUnit { pushApi corresAlarm pov }
    case lila.notify.PushNotification(to, content, _) =>
      logUnit { pushApi notifyPush (to, content) }
    case t: lila.hub.actorApi.push.TourSoon =>
      logUnit { pushApi tourSoon t }

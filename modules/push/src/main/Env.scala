package lila.push

import akka.actor.*
import com.softwaremill.macwire.*
import com.softwaremill.tagging.*
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient

import lila.common.Bus
import lila.common.autoconfig.{ *, given }
import lila.core.config.*

@Module
final private class PushConfig(
    @ConfigName("collection.device") val deviceColl: CollName,
    @ConfigName("collection.subscription") val subscriptionColl: CollName,
    @ConfigName("collection.unifiedpush") val unifiedPushColl: CollName,
    val web: WebPush.Config,
    val firebase: FirebasePush.BothConfigs
)

trait BrowserSub
trait UnifiedSub

@Module
final class Env(
    appConfig: Configuration,
    ws: StandaloneWSClient,
    db: lila.db.Db,
    gameProxy: lila.core.game.GameProxy,
    roundJson: lila.core.round.RoundJson,
    gameRepo: lila.core.game.GameRepo,
    namer: lila.core.game.Namer,
    notifyAllows: lila.core.notify.GetNotifyAllows,
    postApi: lila.core.forum.ForumPostApi,
    getLightUser: lila.core.LightUser.GetterFallback
)(using Executor, Scheduler):

  private val config = appConfig.get[PushConfig]("push")(using AutoConfig.loader)

  def vapidPublicKey = config.web.vapidPublicKey

  private val deviceApi = DeviceApi(db(config.deviceColl))

  val browserSub = WebSubscriptionApi(db(config.subscriptionColl)).taggedWith[BrowserSub]
  val unifiedSub = WebSubscriptionApi(db(config.unifiedPushColl)).taggedWith[UnifiedSub]

  export deviceApi.{
    register as registerDevice,
    unregisterAllForUser as unregisterAllDevicesForUser,
    findLastManyByUserId as findDevicesByUserId,
    findByDeviceId as findDevice,
    delete as deleteDevice
  }

  type Device = lila.push.Device

  private lazy val browserPush = wire[BrowserWebPush]
  private lazy val unifiedPush = wire[UnifiedWebPush]
  private lazy val firebasePush = wire[FirebasePush]
  private lazy val pushApi = wire[PushApi]

  Bus.sub[lila.core.misc.oauth.TokenRevoke]: token =>
    unifiedSub.unsubscribeBySession(token.id)

  Bus.sub[lila.core.game.FinishGame]: f =>
    logUnit { pushApi.finish(f.game) }

  Bus.sub[lila.core.round.CorresMoveEvent]:
    case lila.core.round.CorresMoveEvent(move, _, pushable, _, _) if pushable =>
      logUnit { pushApi.move(move) }

  Bus.sub[lila.core.round.CorresTakebackOfferEvent]: e =>
    logUnit { pushApi.takebackOffer(e.gameId) }

  Bus.sub[lila.core.round.CorresDrawOfferEvent]: e =>
    logUnit { pushApi.drawOffer(e.gameId) }

  Bus.sub[lila.core.challenge.PositiveEvent]:
    case lila.core.challenge.PositiveEvent.Create(c) =>
      logUnit { pushApi.challengeCreate(c) }
    case lila.core.challenge.PositiveEvent.Accept(c, game, joinerId) =>
      logUnit { pushApi.challengeAccept(c, game, joinerId) }

  Bus.sub[lila.core.game.CorresAlarmEvent]: e =>
    logUnit { pushApi.corresAlarm(e.pov) }

  Bus.sub[lila.core.notify.PushNotification]: n =>
    logUnit { pushApi.notifyPush(n.to, n.content) }

  Bus.sub[lila.core.misc.push.TourSoon]: t =>
    logUnit { pushApi.tourSoon(t) }

  private def logUnit(f: Fu[?]): Unit =
    f.logFailure(logger)
    ()

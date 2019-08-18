package lidraughts.plan

import com.typesafe.config.Config
import lidraughts.memo.SettingStore.{ StringReader, Formable }
import scala.concurrent.duration._

final class Env(
    config: Config,
    db: lidraughts.db.Env,
    hub: lidraughts.hub.Env,
    notifyApi: lidraughts.notify.NotifyApi,
    system: akka.actor.ActorSystem,
    asyncCache: lidraughts.memo.AsyncCache.Builder,
    lightUserApi: lidraughts.user.LightUserApi,
    settingStore: lidraughts.memo.SettingStore.Builder,
    scheduler: lidraughts.common.Scheduler
) {

  val stripePublicKey = config getString "stripe.keys.public"

  private val CollectionPatron = config getString "collection.patron"
  private val CollectionCharge = config getString "collection.charge"

  private lazy val patronColl = db(CollectionPatron)
  private lazy val chargeColl = db(CollectionCharge)

  private lazy val stripeClient = new StripeClient(StripeClient.Config(
    endpoint = config getString "stripe.endpoint",
    publicKey = stripePublicKey,
    secretKey = config getString "stripe.keys.secret"
  ))

  private lazy val notifier = new PlanNotifier(
    notifyApi = notifyApi,
    scheduler = scheduler,
    timeline = hub.timeline
  )

  private lazy val monthlyGoalApi = new MonthlyGoalApi(
    getGoal = () => Eur(donationGoalSetting.get()),
    chargeColl = chargeColl
  )

  val donationGoalSetting = settingStore[Int](
    "donationGoal",
    default = 100,
    text = "Monthly donation goal in EUR".some
  )

  lazy val api = new PlanApi(
    stripeClient,
    patronColl = patronColl,
    chargeColl = chargeColl,
    notifier = notifier,
    lightUserApi = lightUserApi,
    asyncCache = asyncCache,
    payPalIpnKey = PayPalIpnKey(config getString "paypal.ipn_key"),
    monthlyGoalApi = monthlyGoalApi
  )(system)

  private lazy val webhookHandler = new WebhookHandler(api)

  private lazy val expiration = new Expiration(patronColl, notifier)

  scheduler.future(15 minutes, "Expire patron plans") {
    expiration.run
  }

  def webhook = webhookHandler.apply _

  def cli = new lidraughts.common.Cli {
    def process = {
      case "patron" :: "lifetime" :: user :: Nil =>
        lidraughts.user.UserRepo named user flatMap { _ ?? api.setLifetime } inject "ok"
      // someone donated while logged off.
      // we cannot bind the charge to the user so they get their precious wings.
      // instead, give them a free month.
      case "patron" :: "month" :: user :: Nil =>
        lidraughts.user.UserRepo named user flatMap { _ ?? api.giveMonth } inject "ok"
    }
  }
}

object Env {

  lazy val current: Env = "plan" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "plan",
    db = lidraughts.db.Env.current,
    hub = lidraughts.hub.Env.current,
    notifyApi = lidraughts.notify.Env.current.api,
    lightUserApi = lidraughts.user.Env.current.lightUserApi,
    system = lidraughts.common.PlayApp.system,
    asyncCache = lidraughts.memo.Env.current.asyncCache,
    settingStore = lidraughts.memo.Env.current.settingStore,
    scheduler = lidraughts.common.PlayApp.scheduler
  )
}

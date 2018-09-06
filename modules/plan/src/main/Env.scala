package lidraughts.plan

import com.typesafe.config.Config
import scala.concurrent.duration._

final class Env(
    config: Config,
    db: lidraughts.db.Env,
    hub: lidraughts.hub.Env,
    notifyApi: lidraughts.notify.NotifyApi,
    bus: lidraughts.common.Bus,
    asyncCache: lidraughts.memo.AsyncCache.Builder,
    lightUserApi: lidraughts.user.LightUserApi,
    scheduler: lidraughts.common.Scheduler
) {

  val stripePublicKey = config getString "stripe.keys.public"

  private val CollectionPatron = config getString "collection.patron"
  private val CollectionCharge = config getString "collection.charge"
  private val MonthlyGoalCents = Usd(config getInt "monthly_goal").cents

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
    timeline = hub.actor.timeline
  )

  private lazy val monthlyGoalApi = new MonthlyGoalApi(
    goal = MonthlyGoalCents,
    chargeColl = chargeColl
  )

  lazy val api = new PlanApi(
    stripeClient,
    patronColl = patronColl,
    chargeColl = chargeColl,
    notifier = notifier,
    lightUserApi = lightUserApi,
    bus = bus,
    asyncCache = asyncCache,
    payPalIpnKey = PayPalIpnKey(config getString "paypal.ipn_key"),
    monthlyGoalApi = monthlyGoalApi
  )

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
    bus = lidraughts.common.PlayApp.system.lidraughtsBus,
    asyncCache = lidraughts.memo.Env.current.asyncCache,
    scheduler = lidraughts.common.PlayApp.scheduler
  )
}

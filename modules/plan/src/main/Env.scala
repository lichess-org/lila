package lila.plan

import com.typesafe.config.Config
import scala.concurrent.duration._

final class Env(
    config: Config,
    db: lila.db.Env,
    hub: lila.hub.Env,
    notifyApi: lila.notify.NotifyApi,
    bus: lila.common.Bus,
    asyncCache: lila.memo.AsyncCache.Builder,
    lightUserApi: lila.user.LightUserApi,
    scheduler: lila.common.Scheduler
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

  def cli = new lila.common.Cli {
    def process = {
      case "patron" :: "lifetime" :: user :: Nil =>
        lila.user.UserRepo named user flatMap { _ ?? api.setLifetime } inject "ok"
    }
  }
}

object Env {

  lazy val current: Env = "plan" boot new Env(
    config = lila.common.PlayApp loadConfig "plan",
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    notifyApi = lila.notify.Env.current.api,
    lightUserApi = lila.user.Env.current.lightUserApi,
    bus = lila.common.PlayApp.system.lilaBus,
    asyncCache = lila.memo.Env.current.asyncCache,
    scheduler = lila.common.PlayApp.scheduler
  )
}

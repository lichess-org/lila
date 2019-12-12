package lila.plan

import com.typesafe.config.Config
import lila.memo.SettingStore.{ StringReader, Formable }
import scala.concurrent.duration._

final class Env(
    config: Config,
    db: lila.db.Env,
    hub: lila.hub.Env,
    notifyApi: lila.notify.NotifyApi,
    system: akka.actor.ActorSystem,
    asyncCache: lila.memo.AsyncCache.Builder,
    lightUserApi: lila.user.LightUserApi,
    settingStore: lila.memo.SettingStore.Builder,
    scheduler: lila.common.Scheduler
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
    getGoal = () => Usd(donationGoalSetting.get()),
    chargeColl = chargeColl
  )

  val donationGoalSetting = settingStore[Int](
    "donationGoal",
    default = 10 * 1000,
    text = "Monthly donation goal in USD from https://lichess.org/costs".some
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

  def cli = new lila.common.Cli {
    def process = {
      case "patron" :: "lifetime" :: user :: Nil =>
        lila.user.UserRepo named user flatMap { _ ?? api.setLifetime } inject "ok"
      // someone donated while logged off.
      // we cannot bind the charge to the user so they get their precious wings.
      // instead, give them a free month.
      case "patron" :: "month" :: user :: Nil =>
        lila.user.UserRepo named user flatMap { _ ?? api.giveMonth } inject "ok"
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
    system = lila.common.PlayApp.system,
    asyncCache = lila.memo.Env.current.asyncCache,
    settingStore = lila.memo.Env.current.settingStore,
    scheduler = lila.common.PlayApp.scheduler
  )
}

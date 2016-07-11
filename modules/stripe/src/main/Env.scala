package lila.stripe

import akka.actor._
import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env,
    bus: lila.common.Bus) {

  val publicKey = config getString "keys.public"
  private val CollectionPatron = config getString "collection.patron"

  private lazy val client = new StripeClient(StripeClient.Config(
    endpoint = config getString "endpoint",
    publicKey = publicKey,
    secretKey = config getString "keys.secret"))

  lazy val api = new StripeApi(client, db(CollectionPatron), bus)

  private lazy val webhookHandler = new WebhookHandler(api)

  def webhook = webhookHandler.apply _
}

object Env {

  lazy val current: Env = "stripe" boot new Env(
    config = lila.common.PlayApp loadConfig "stripe",
    db = lila.db.Env.current,
    bus = lila.common.PlayApp.system.lilaBus)
}

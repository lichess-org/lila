package lila.stripe

import akka.actor._
import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(config: Config, db: lila.db.Env) {

  val publicKey = config getString "keys.public"
  private val CollectionCustomer = config getString "collection.customer"

  private lazy val client = new StripeClient(StripeClient.Config(
    endpoint = config getString "endpoint",
    publicKey = publicKey,
    secretKey = config getString "keys.secret"))

  lazy val api = new StripeApi(client, db(CollectionCustomer))

  private lazy val webhookHandler = new WebhookHandler(api)

  def webhook = webhookHandler.apply _
}

object Env {

  lazy val current: Env = "stripe" boot new Env(
    // system = lila.common.PlayApp.system,
    // getLightUser = lila.user.Env.current.lightUser,
    config = lila.common.PlayApp loadConfig "stripe",
  db = lila.db.Env.current)
}

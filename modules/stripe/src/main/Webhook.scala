package lila.stripe

import play.api.libs.json._

private final class WebhookHandler(api: StripeApi) {

  import JsonHandlers._

  def apply(js: JsValue): Funit = ~{
    logger.debug(s"Webhook ${js.toString.take(80)}")
    for {
      name <- (js \ "type").asOpt[String]
      data <- (js \ "data" \ "object").asOpt[JsObject]
    } yield (name match {
      case "charge.succeeded" =>
        val charge = data.asOpt[StripeCharge] err s"Invalid charge $data"
        api onCharge charge
      case "customer.subscription.deleted" =>
        val sub = data.asOpt[StripeSubscription] err s"Invalid subscription $data"
        api onSubscriptionDeleted sub
      case typ => funit
    })
  }
}

package lila.plan

import play.api.libs.json._

final class WebhookHandler(api: PlanApi)(implicit ec: scala.concurrent.ExecutionContext) {

  import JsonHandlers._

  // Never trust an incoming webhook call.
  // Only read the Event ID from it,
  // then fetch the event from the stripe API.
  def apply(js: JsValue): Funit =
    (js \ "id").asOpt[String] ?? api.getEvent flatMap {
      case None =>
        logger.warn(s"Forged webhook $js")
        funit
      case Some(event) =>
        ~(for {
          name <- (event \ "type").asOpt[String]
          data <- (event \ "data" \ "object").asOpt[JsObject]
        } yield (name match {
          case "charge.succeeded" =>
            val charge = data.asOpt[StripeCharge] err s"Invalid charge $data"
            api onStripeCharge charge
          case "customer.subscription.deleted" =>
            val sub = data.asOpt[StripeSubscription] err s"Invalid subscription $data"
            api onSubscriptionDeleted sub
          case "checkout.session.completed" =>
            val sub = data.asOpt[StripeCompletedSession] err s"Invalid session completed $data"
            api onCompletedSession sub
          case _ => funit
        }))
    }

}

package lila.plan

import play.api.libs.json._

final class WebhookHandler(api: PlanApi)(implicit ec: scala.concurrent.ExecutionContext) {

  import JsonHandlers._

  // Never trust an incoming webhook call.
  // Only read the Event ID from it,
  // then fetch the event from the stripe API.
  def stripe(js: JsValue): Funit = {
    def log = logger branch "stripe.webhook"
    (js \ "id").asOpt[String] ?? api.getEvent flatMap {
      case None =>
        log.warn(s"Forged $js")
        funit
      case Some(event) =>
        import JsonHandlers.stripe._
        ~(for {
          id   <- (event \ "id").asOpt[String]
          name <- (event \ "type").asOpt[String]
          data <- (event \ "data" \ "object").asOpt[JsObject]
        } yield {
          log.debug(s"$name $id ${Json.stringify(data).take(100)}")
          name match {
            case "customer.subscription.deleted" =>
              val sub = data.asOpt[StripeSubscription] err s"Invalid subscription $data"
              api onSubscriptionDeleted sub
            case "charge.succeeded" =>
              val charge = data.asOpt[StripeCharge] err s"Invalid charge $data"
              api onStripeCharge charge
            case _ => funit
          }
        })
    }
  }

  def payPal(js: JsValue): Funit = ~ {
    def log = logger branch "payPal.webhook"
    println(js)
    for {
      id   <- (js \ "id").asOpt[String]
      tpe  <- (js \ "event_type").asOpt[String]
      data <- (js \ "resource").asOpt[JsObject]
    } yield {
      println(data)
      log.debug(s"$tpe $id ${Json.stringify(data).take(100)}")
      funit
    }
  }

}

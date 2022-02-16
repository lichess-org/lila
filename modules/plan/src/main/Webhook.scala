package lila.plan

import play.api.libs.json._

final class WebhookHandler(api: PlanApi)(implicit ec: scala.concurrent.ExecutionContext) {

  import JsonHandlers._

  // Never trust an incoming webhook call.
  // Only read the Event ID from it,
  // then fetch the event from the stripe API.
  def stripe(js: JsValue): Funit = {
    def log = logger branch "stripe.webhook"
    js.str("id") ?? api.getEvent flatMap {
      case None =>
        log.warn(s"Forged $js")
        funit
      case Some(event) =>
        import JsonHandlers.stripe._
        ~(for {
          id   <- event str "id"
          name <- event str "type"
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
    for {
      id   <- js str "id"
      tpe  <- js str "event_type"
      data <- js obj "resource"
    } yield {
      println(data)
      log.debug(s"$tpe $id ${Json.stringify(data).take(100)}")
      tpe match {
        case "BILLING.SUBSCRIPTION.ACTIVATED" => funit
        // {
        //   for {
        //     userId <- data.str("custom_id")
        //     subId  <- data.str("id")
        //   } yield api.paypalSubscriptionActivated(PayPalSubscriptionId(subId), userId)
        // } | fufail(s"Invalid PayPal webhook $data")
        case _ => funit
      }
    }
  }

}

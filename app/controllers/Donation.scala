package controllers

import play.api.mvc._, Results._

import lila.app._
import views._

object Donation extends LilaController {

  def index = Open { implicit ctx =>
    OptionFuOk(Prismic.oneShotBookmark("donate")) {
      case (doc, resolver) => Env.donation.api.list map { donations =>
        views.html.donation.index(doc, resolver, donations)
      }
    }
  }

  def thanks = Open { implicit ctx =>
    OptionOk(Prismic.oneShotBookmark("donate-thanks")) {
      case (doc, resolver) => views.html.donation.thanks(doc, resolver)
    }
  }

  def ipn = Action { req =>
    println(req.queryString)
    val donation = for {
      txn <- get("txn_id", req)
      txnType <- get("txn_type", req)
      if Set("express_checkout", "web_accept", "recurring_payment")(txnType)
      amount <- get("mc_gross", req) flatMap parseFloatOption
      userId = get("custom", req)
      email = get("payer_email", req)
      firstName = get("first_name", req)
      lastName = get("last_name", req)
      name = (firstName |@| lastName) apply { _ + " " + _ }
    } yield lila.donation.Donation.make(
      payPalTnx = txn.some,
      userId = userId,
      email = email,
      name = name,
      amount = (amount * 100).toInt,
      message = "")
    donation match {
      case Some(d) =>
        println("Created donation " + d)
        Env.donation.api create d
      case None =>
        println("Failed to create donation")
    }
    Ok
  }
}

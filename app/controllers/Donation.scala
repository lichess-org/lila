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

  def ipn = Action.async { implicit req =>
    Env.donation.forms.ipn.bindFromRequest.fold(
      err => {
        println(err)
        fuccess(BadRequest)
      },
      ipn => {
        val donation = lila.donation.Donation.make(
          payPalTnx = ipn.txnId.some,
          userId = ipn.userId,
          email = ipn.email,
          name = ipn.name,
          amount = ipn.cents,
          message = "")
        println(donation)
        Env.donation.api create donation inject Ok
      })
  }
}

package controllers

import play.api.mvc._, Results._

import lila.app._
import views._

object Donation extends LilaController {

  def index = Open { implicit ctx =>
    OptionFuOk(Prismic.getBookmark("donate")) {
      case (doc, resolver) => Env.donation.api.list(100) zip
        Env.donation.api.top(10) map {
          case (donations, top) =>
            views.html.donation.index(doc, resolver, donations, top)
        }
    }
  }

  def thanks = Open { implicit ctx =>
    Redirect(routes.Plan.index).fuccess
  }

  def thanksRedirect = Action(Redirect(routes.Donation.thanks))

  def ipn = Action.async { implicit req =>
    Env.donation.forms.ipn.bindFromRequest.fold(
      err => {
        println(err)
        fuccess(Ok)
      },
      ipn => {
        val donation = lila.donation.Donation.make(
          payPalTnx = ipn.txnId,
          payPalSub = ipn.subId,
          userId = ipn.userId,
          email = ipn.email,
          name = ipn.name,
          gross = ipn.grossCents,
          fee = ipn.feeCents,
          message = "")
        ipn.userId.?? { userId =>
          import lila.plan.Patron.PayPal
          Env.plan.api.onPaypalCharge(
            userId = userId,
            email = ipn.email map PayPal.Email.apply,
            subId = ipn.subId map PayPal.SubId.apply,
            cents = lila.plan.Cents(ipn.grossCents),
            name = ipn.name,
            txnId = ipn.txnId)
        } >>
          Env.donation.api.create(donation) inject Ok
      })
  }
}

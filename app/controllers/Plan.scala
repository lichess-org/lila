package controllers

import play.api.libs.json._
import play.api.mvc._, Results._

import lila.app._
import views._

object Plan extends LilaController {

  def index = Open { implicit ctx =>
    (ctx.userId ?? lila.user.UserRepo.email) map { myEmail =>
      html.plan.index(
        myEmail = myEmail,
        stripePublicKey = Env.stripe.publicKey)
    }
  }

  def features = Open { implicit ctx =>
    fuccess {
      html.plan.features()
    }
  }

  def charge = AuthBody { implicit ctx =>
    me =>
      implicit val req = ctx.body
      import lila.stripe.StripeClient._
      lila.stripe.Checkout.form.bindFromRequest.fold(
        err => BadRequest(html.plan.badCheckout(err.toString)).fuccess,
        data => Env.stripe.api.checkout(me, data) map { res =>
          Ok(html.plan.thanks())
        } recover {
          case e: StripeException =>
            lila.log("stripe").error("Plan.charge", e)
            BadRequest(html.plan.badCheckout(e.toString))
        }
      )
  }

  def thanks = Open { implicit ctx =>
    fuccess {
      html.plan.thanks()
    }
  }

  def webhook = Action.async(parse.json) { req =>
    Env.stripe.webhook(req.body) map { _ => Ok("kthxbye") }
  }
}

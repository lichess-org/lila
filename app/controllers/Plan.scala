package controllers

import play.api.libs.json._
import play.api.mvc._, Results._

import lila.api.Context
import lila.app._
import lila.user.{ User => UserModel }
import views._

object Plan extends LilaController {

  def index = Open { implicit ctx =>
    ctx.me.fold(indexAnon) { me =>
      Env.stripe.api.sync(me) flatMap {
        case true                => Redirect(routes.Plan.index).fuccess
        case _ if me.plan.active => indexCustomer(me)
        case _                   => indexFreeUser(me)
      }
    }
  }

  private def indexAnon(implicit ctx: Context) =
    Ok(html.plan.indexAnon()).fuccess

  private def indexCustomer(me: UserModel)(implicit ctx: Context) =
    Env.stripe.api.customerInfo(me) flatMap {
      case Some(info) => Ok(html.plan.indexCustomer(me, info)).fuccess
      case _          => indexFreeUser(me)
    }

  private def indexFreeUser(me: UserModel)(implicit ctx: Context) =
    lila.user.UserRepo email me.id map { myEmail =>
      Ok(html.plan.indexFreeUser(me,
        myEmail = myEmail,
        stripePublicKey = Env.stripe.publicKey))
    }

  def features = Open { implicit ctx =>
    fuccess {
      html.plan.features()
    }
  }

  def switch = AuthBody { implicit ctx =>
    me =>
      implicit val req = ctx.body
      lila.stripe.Switch.form.bindFromRequest.fold(
        err => Redirect(routes.Plan.index).fuccess,
        data => (data.plan.flatMap(lila.stripe.LichessPlan.byId) match {
          case Some(plan)                 => Env.stripe.api.switch(me, plan)
          case _ if data.cancel.isDefined => Env.stripe.api.cancel(me)
          case _                          => fufail("Invalid switch")
        }) recover {
          case e: Exception =>
            lila.log("stripe").error("Plan.switch", e)
            Redirect(routes.Plan.index)
        } inject Redirect(routes.Plan.index())
      )
  }

  def charge = AuthBody { implicit ctx =>
    me =>
      implicit val req = ctx.body
      import lila.stripe.StripeClient._
      lila.stripe.Checkout.form.bindFromRequest.fold(
        err => BadRequest(html.plan.badCheckout(err.toString)).fuccess,
        data => Env.stripe.api.checkout(me, data) map { res =>
          Redirect(routes.Plan.index() + "?thanks=1")
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

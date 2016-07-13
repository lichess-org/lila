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
      Env.plan.api.sync(me) flatMap {
        case true                => Redirect(routes.Plan.index).fuccess
        case _ if me.plan.active => indexPatron(me)
        case _                   => indexFreeUser(me)
      }
    }
  }

  private def indexAnon(implicit ctx: Context) = renderIndex(email = none)

  private def indexFreeUser(me: UserModel)(implicit ctx: Context) =
    lila.user.UserRepo email me.id flatMap renderIndex

  private def renderIndex(email: Option[String])(implicit ctx: Context): Fu[Result] =
    Ok(html.plan.index(
      stripePublicKey = Env.plan.stripePublicKey,
      email = email)).fuccess

  private def indexPatron(me: UserModel)(implicit ctx: Context) =
    Env.plan.api.customerInfo(me) flatMap {
      case Some(info) => Ok(html.plan.indexStripe(me, info)).fuccess
      case _          => Ok(html.plan.indexPaypal(me)).fuccess
    }

  def features = Open { implicit ctx =>
    fuccess {
      html.plan.features()
    }
  }

  def switch = AuthBody { implicit ctx =>
    me =>
      implicit val req = ctx.body
      lila.plan.Switch.form.bindFromRequest.fold(
        err => funit,
        data => Env.plan.api.switch(me, data.cents)
      ) inject Redirect(routes.Plan.index)
  }

  def cancel = AuthBody { implicit ctx =>
    me =>
      Env.plan.api.cancel(me) inject Redirect(routes.Plan.index())
  }

  def charge = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    import lila.plan.StripeClient._
    lila.plan.Checkout.form.bindFromRequest.fold(
      err => BadRequest(html.plan.badCheckout(err.toString)).fuccess,
      data => Env.plan.api.checkout(ctx.me, data) inject Redirect {
        if (ctx.isAuth) {
          if (data.isMonthly) routes.Plan.index()
          else routes.Plan.thanks()
        }
        else routes.Plan.thanks()
      } recover {
        case e: StripeException =>
          lila.log("plan").error("Plan.charge", e)
          BadRequest(html.plan.badCheckout(e.toString))
      }
    )
  }

  def thanks = Open { implicit ctx =>
    OptionOk(Prismic.getBookmark("donate-thanks")) {
      case (doc, resolver) => views.html.site.page(doc, resolver)
    }
  }

  def webhook = Action.async(parse.json) { req =>
    Env.plan.webhook(req.body) map { _ => Ok("kthxbye") }
  }
}

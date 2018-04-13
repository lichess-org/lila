package controllers

import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.plan.{ StripeCustomer, MonthlyCustomerInfo, OneTimeCustomerInfo }
import lila.common.EmailAddress
import lila.user.{ User => UserModel, UserRepo }
import views._

object Plan extends LilaController {

  private val logger = lila.log("plan")

  def index = Open { implicit ctx =>
    pageHit
    ctx.me.fold(indexAnon) { me =>
      import lila.plan.PlanApi.SyncResult._
      Env.plan.api.sync(me) flatMap {
        case ReloadUser => Redirect(routes.Plan.index).fuccess
        case Synced(Some(patron), None) => UserRepo email me.id flatMap { email =>
          renderIndex(email, patron.some)
        }
        case Synced(Some(patron), Some(customer)) => indexPatron(me, patron, customer)
        case Synced(_, _) => indexFreeUser(me)
      }
    }
  }

  def list = Open { implicit ctx =>
    ctx.me.fold(Redirect(routes.Plan.index).fuccess) { me =>
      import lila.plan.PlanApi.SyncResult._
      Env.plan.api.sync(me) flatMap {
        case ReloadUser => Redirect(routes.Plan.list).fuccess
        case Synced(Some(_), _) => indexFreeUser(me)
        case _ => Redirect(routes.Plan.index).fuccess
      }
    }
  }

  private def indexAnon(implicit ctx: Context) = renderIndex(email = none, patron = none)

  private def indexFreeUser(me: UserModel)(implicit ctx: Context) =
    UserRepo email me.id flatMap { email =>
      renderIndex(email, patron = none)
    }

  private def renderIndex(email: Option[EmailAddress], patron: Option[lila.plan.Patron])(implicit ctx: Context): Fu[Result] = for {
    recentIds <- Env.plan.api.recentChargeUserIds
    bestIds <- Env.plan.api.topPatronUserIds
    _ <- Env.user.lightUserApi preloadMany { recentIds ::: bestIds }
  } yield Ok(html.plan.index(
    stripePublicKey = Env.plan.stripePublicKey,
    email = email,
    patron = patron,
    recentIds = recentIds,
    bestIds = bestIds
  ))

  private def indexPatron(me: UserModel, patron: lila.plan.Patron, customer: StripeCustomer)(implicit ctx: Context) =
    Env.plan.api.customerInfo(me, customer) flatMap {
      case Some(info: MonthlyCustomerInfo) => Ok(html.plan.indexStripe(me, patron, info)).fuccess
      case Some(info: OneTimeCustomerInfo) => renderIndex(info.customer.email map EmailAddress.apply, patron.some)
      case None => UserRepo email me.id flatMap { email =>
        renderIndex(email, patron.some)
      }
    }

  def features = Open { implicit ctx =>
    pageHit
    fuccess {
      html.plan.features()
    }
  }

  def switch = AuthBody { implicit ctx => me =>
    implicit val req = ctx.body
    lila.plan.Switch.form.bindFromRequest.fold(
      err => funit,
      data => Env.plan.api.switch(me, data.cents)
    ) inject Redirect(routes.Plan.index)
  }

  def cancel = AuthBody { implicit ctx => me =>
    Env.plan.api.cancel(me) inject Redirect(routes.Plan.index())
  }

  def charge = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    import lila.plan.StripeClient._
    lila.plan.Checkout.form.bindFromRequest.fold(
      err => BadRequest(html.plan.badCheckout(err.toString)).fuccess,
      data => Env.plan.api.checkout(ctx.me, data) inject Redirect {
        if (ctx.isAuth) {
          if (data.freq.renew) routes.Plan.index()
          else routes.Plan.thanks()
        } else routes.Plan.thanks()
      } recover {
        case e: StripeException =>
          logger.error("Plan.charge", e)
          BadRequest(html.plan.badCheckout(e.toString))
      }
    )
  }

  def thanks = Open { implicit ctx =>
    ctx.me ?? Env.plan.api.userPatron flatMap { patron =>
      patron ?? Env.plan.api.patronCustomer map { customer =>
        Ok(html.plan.thanks(patron, customer))
      }
    }
  }

  def webhook = Action.async(parse.json) { req =>
    Env.plan.webhook(req.body) map { _ => Ok("kthxbye") }
  }

  def payPalIpn = Action.async { implicit req =>
    import lila.plan.Patron.PayPal
    lila.plan.DataForm.ipn.bindFromRequest.fold(
      err => {
        if (err.errors("txn_type").nonEmpty) {
          logger.debug(s"Plan.payPalIpn ignore txn_type = ${err.data get "txn_type"}")
          fuccess(Ok)
        } else {
          logger.error(s"Plan.payPalIpn invalid data ${err.toString}")
          fuccess(BadRequest)
        }
      },
      ipn => Env.plan.api.onPaypalCharge(
        userId = ipn.userId,
        email = ipn.email map PayPal.Email.apply,
        subId = ipn.subId map PayPal.SubId.apply,
        cents = lila.plan.Cents(ipn.grossCents),
        name = ipn.name,
        txnId = ipn.txnId,
        ip = lila.common.HTTPRequest.lastRemoteAddress(req).value,
        key = lila.plan.PayPalIpnKey(get("key", req) | "N/A")
      ) inject Ok
    )
  }
}

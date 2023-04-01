package controllers

import java.util.Currency
import play.api.libs.json.*
import play.api.mvc.*

import lila.api.Context
import lila.app.{ given, * }
import lila.common.EmailAddress
import lila.plan.{
  CreateStripeSession,
  Freq,
  Money,
  CustomerInfo,
  NextUrls,
  PayPalOrderId,
  PayPalSubscription,
  PayPalSubscriptionId,
  PlanCheckout,
  StripeCustomer,
  StripeCustomerId
}
import lila.user.{ User as UserModel }
import views.*

final class Plan(env: Env) extends LilaController(env):

  private val logger = lila.log("plan")

  def index =
    Open { implicit ctx =>
      pageHit
      ctx.me.fold(indexAnon) { me =>
        import lila.plan.PlanApi.SyncResult.*
        env.plan.api.sync(me) flatMap {
          case ReloadUser => Redirect(routes.Plan.index).toFuccess
          case Synced(Some(patron), None, None) =>
            env.user.repo email me.id flatMap { email =>
              renderIndex(email, patron.some)
            }
          case Synced(Some(patron), Some(stripeCus), _) => indexStripePatron(me, patron, stripeCus)
          case Synced(Some(patron), _, Some(payPalSub)) => indexPayPalPatron(me, patron, payPalSub)
          case _                                        => indexFreeUser(me)
        }
      }
    }

  def list =
    Open { implicit ctx =>
      ctx.me.fold(Redirect(routes.Plan.index).toFuccess) { me =>
        import lila.plan.PlanApi.SyncResult.*
        env.plan.api.sync(me) flatMap {
          case ReloadUser            => Redirect(routes.Plan.list).toFuccess
          case Synced(Some(_), _, _) => indexFreeUser(me)
          case _                     => Redirect(routes.Plan.index).toFuccess
        }
      }
    }

  private def indexAnon(implicit ctx: Context) = renderIndex(email = none, patron = none)

  private def indexFreeUser(me: UserModel)(implicit ctx: Context) =
    env.user.repo email me.id flatMap { email =>
      renderIndex(email, patron = none)
    }

  private def renderIndex(email: Option[EmailAddress], patron: Option[lila.plan.Patron])(using
      Context
  ): Fu[Result] =
    for {
      recentIds <- env.plan.api.recentChargeUserIds
      bestIds   <- env.plan.api.topPatronUserIds
      _         <- env.user.lightUserApi.preloadMany(recentIds ::: bestIds)
      pricing   <- env.plan.priceApi.pricingOrDefault(myCurrency)
    } yield Ok(
      html.plan.index(
        stripePublicKey = env.plan.stripePublicKey,
        payPalPublicKey = env.plan.payPalPublicKey,
        email = email,
        patron = patron,
        recentIds = recentIds,
        bestIds = bestIds,
        pricing = pricing
      )
    )

  private def indexStripePatron(me: UserModel, patron: lila.plan.Patron, customer: StripeCustomer)(using
      ctx: Context
  ) = for {
    pricing <- env.plan.priceApi.pricingOrDefault(myCurrency)
    info    <- env.plan.api.stripe.customerInfo(me, customer)
    gifts   <- env.plan.api.giftsFrom(me)
    res <- info match
      case Some(info: CustomerInfo.Monthly) =>
        Ok(html.plan.indexStripe(me, patron, info, env.plan.stripePublicKey, pricing, gifts)).toFuccess
      case Some(CustomerInfo.OneTime(cus)) =>
        renderIndex(cus.email map { EmailAddress(_) }, patron.some)
      case None =>
        env.user.repo email me.id flatMap { email =>
          renderIndex(email, patron.some)
        }
  } yield res

  private def indexPayPalPatron(me: UserModel, patron: lila.plan.Patron, sub: PayPalSubscription)(using
      Context
  ) =
    env.plan.api.giftsFrom(me) map { gifts =>
      Ok(html.plan.indexPayPal(me, patron, sub, gifts))
    }

  private def myCurrency(implicit ctx: Context): Currency =
    get("currency") flatMap lila.plan.CurrencyApi.currencyOption getOrElse
      env.plan.currencyApi.currencyByCountryCodeOrLang(
        env.security.geoIP(ctx.ip).flatMap(_.countryCode),
        ctx.lang
      )

  def features =
    Open { implicit ctx =>
      pageHit
      fuccess {
        html.plan.features()
      }
    }

  def switch =
    AuthBody { implicit ctx => me =>
      given play.api.mvc.Request[?] = ctx.body
      env.plan.priceApi.pricingOrDefault(myCurrency) flatMap { pricing =>
        lila.plan.Switch
          .form(pricing)
          .bindFromRequest()
          .fold(
            _ => funit,
            data => env.plan.api.switch(me, data.money)
          ) inject Redirect(routes.Plan.index)
      }
    }

  def cancel =
    AuthBody { _ => me =>
      env.plan.api.cancel(me) inject Redirect(routes.Plan.index)
    }

  def thanks =
    Open { implicit ctx =>
      // wait for the payment data from stripe or paypal
      lila.common.LilaFuture.delay(2.seconds) {
        for {
          patron   <- ctx.me ?? env.plan.api.userPatron
          customer <- patron ?? env.plan.api.stripe.patronCustomer
          gift     <- ctx.me ?? env.plan.api.recentGiftFrom
        } yield Ok(html.plan.thanks(patron, customer, gift))
      }
    }

  def webhook =
    Action.async(parse.json) { req =>
      if (req.headers.hasHeader("PAYPAL-TRANSMISSION-SIG"))
        env.plan.webhook.payPal(req.body) inject Ok("kthxbye")
      else
        env.plan.webhook.stripe(req.body) inject Ok("kthxbye")
    }

  import lila.plan.StripeClient.{ StripeException, CantUseException }
  def badStripeApiCall: PartialFunction[Throwable, Result] = {
    case e @ CantUseException => BadRequest(jsonError(e.getMessage))
    case e: StripeException =>
      logger.error("Plan.stripeCheckout", e)
      BadRequest(jsonError("Stripe API call failed"))
  }

  private def createStripeSession(
      me: UserModel,
      checkout: PlanCheckout,
      customerId: StripeCustomerId,
      giftTo: Option[UserModel]
  )(using ctx: Context) = {
    for {
      isLifetime <- env.plan.priceApi.isLifetime(checkout.money)
      data = CreateStripeSession(
        customerId,
        checkout,
        NextUrls(
          cancel = s"${env.net.baseUrl}${routes.Plan.index}",
          success = s"${env.net.baseUrl}${routes.Plan.thanks}"
        ),
        giftTo = giftTo,
        isLifetime = isLifetime,
        ip = ctx.ip
      )
      session <- env.plan.api.stripe.createSession(data, me)
    } yield JsonOk(Json.obj("session" -> Json.obj("id" -> session.id.value)))
  }.recover(badStripeApiCall)

  def switchStripePlan(user: UserModel, money: Money) =
    env.plan.api
      .switch(user, money)
      .inject(jsonOkResult)
      .recover(badStripeApiCall)

  private val CheckoutRateLimit = lila.memo.RateLimit.composite[lila.common.IpAddress](
    key = "plan.checkout.ip"
  )(
    ("fast", 8, 10.minute),
    ("slow", 40, 1.day)
  )

  private val CaptureRateLimit = lila.memo.RateLimit.composite[lila.common.IpAddress](
    key = "plan.capture.ip"
  )(
    ("fast", 8, 10.minute),
    ("slow", 40, 1.day)
  )

  def stripeCheckout =
    AuthBody { implicit ctx => me =>
      given play.api.mvc.Request[?] = ctx.body
      CheckoutRateLimit(ctx.ip) {
        env.plan.priceApi.pricingOrDefault(myCurrency) flatMap { pricing =>
          env.plan.checkoutForm
            .form(pricing)
            .bindFromRequest()
            .fold(
              err => {
                logger.info(s"Plan.stripeCheckout 400: $err")
                BadRequest(jsonError(err.errors.map(_.message) mkString ", ")).toFuccess
              },
              data => {
                val checkout = data.fixFreq
                for {
                  gifted   <- checkout.giftTo.filterNot(ctx.userId.has).??(env.user.repo.enabledById)
                  customer <- env.plan.api.stripe.userCustomer(me)
                  session <- customer match {
                    case Some(customer) if checkout.freq == Freq.Onetime =>
                      createStripeSession(me, checkout, customer.id, gifted)
                    case Some(customer) if customer.firstSubscription.isDefined =>
                      switchStripePlan(me, checkout.money)
                    case _ =>
                      env.plan.api.stripe
                        .makeCustomer(me, checkout)
                        .flatMap(customer => createStripeSession(me, checkout, customer.id, gifted))
                  }
                } yield session
              }
            )
        }
      }(rateLimitedFu)
    }

  def updatePayment =
    AuthBody { implicit ctx => me =>
      CaptureRateLimit(ctx.ip) {
        env.plan.api.stripe.userCustomer(me) flatMap {
          _.flatMap(_.firstSubscription).map(_.copy(ip = ctx.ip.some)) ?? { sub =>
            env.plan.api.stripe
              .createPaymentUpdateSession(
                sub,
                NextUrls(
                  cancel = s"${env.net.baseUrl}${routes.Plan.index}",
                  success =
                    s"${env.net.baseUrl}${routes.Plan.updatePaymentCallback}?session={CHECKOUT_SESSION_ID}"
                )
              )
              .map(session => JsonOk(Json.obj("session" -> Json.obj("id" -> session.id.value))))
              .recover(badStripeApiCall)
          }
        }
      }(rateLimitedFu)
    }

  def updatePaymentCallback =
    AuthBody { implicit ctx => me =>
      get("session") ?? { session =>
        env.plan.api.stripe.userCustomer(me) flatMap {
          _.flatMap(_.firstSubscription) ?? { sub =>
            env.plan.api.stripe.updatePaymentMethod(sub, session) inject Redirect(routes.Plan.index)
          }
        }
      }
    }

  def payPalCheckout =
    AuthBody { implicit ctx => me =>
      given play.api.mvc.Request[?] = ctx.body
      CheckoutRateLimit(ctx.ip) {
        env.plan.priceApi.pricingOrDefault(myCurrency) flatMap { pricing =>
          env.plan.checkoutForm
            .form(pricing)
            .bindFromRequest()
            .fold(
              err => {
                logger.info(s"Plan.payPalCheckout 400: $err")
                BadRequest(jsonError(err.errors.map(_.message) mkString ", ")).toFuccess
              },
              data => {
                val checkout = data.fixFreq
                if (checkout.freq.renew) for {
                  sub <- env.plan.api.payPal.createSubscription(checkout, me)
                } yield JsonOk(Json.obj("subscription" -> Json.obj("id" -> sub.id.value)))
                else
                  for {
                    gifted <- checkout.giftTo.filterNot(ctx.userId.has).??(env.user.repo.enabledById)
                    // customer <- env.plan.api.userCustomer(me)
                    order <- env.plan.api.payPal.createOrder(checkout, me, gifted)
                  } yield JsonOk(Json.obj("order" -> Json.obj("id" -> order.id.value)))
              }
            )
        }
      }(rateLimitedFu)
    }

  def payPalCapture(orderId: String) =
    Auth { implicit ctx => me =>
      CaptureRateLimit(ctx.ip) {
        (get("sub") map PayPalSubscriptionId.apply match {
          case None => env.plan.api.payPal.captureOrder(PayPalOrderId(orderId), ctx.ip)
          case Some(subId) =>
            env.plan.api.payPal.captureSubscription(PayPalOrderId(orderId), subId, me, ctx.ip)
        }) inject jsonOkResult
      }(rateLimitedFu)
    }

  // deprecated
  def payPalIpn =
    Action.async { implicit req =>
      lila.plan.PlanForm.ipn
        .bindFromRequest()
        .fold(
          err => {
            if (err.errors("txn_type").nonEmpty) {
              logger.debug(s"Plan.payPalIpn ignore txn_type = ${err.data get "txn_type"}")
              fuccess(Ok)
            } else {
              logger.error(s"Plan.payPalIpn invalid data ${err.toString}")
              fuccess(BadRequest)
            }
          },
          ipn =>
            env.plan.api.payPal.onLegacyCharge(
              ipn,
              ip = req.ipAddress,
              key = get("key", req) | "N/A"
            ) inject Ok
        )
    }

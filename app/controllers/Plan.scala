package controllers

import play.api.libs.json.*
import play.api.mvc.*

import lila.app.{ *, given }
import lila.plan.{
  CustomerInfo,
  Freq,
  Money,
  NextUrls,
  PayPalOrderId,
  PayPalSubscription,
  PayPalSubscriptionId,
  PlanCheckout,
  StripeCustomer,
  StripeCustomerId
}
import lila.common.HTTPRequest

final class Plan(env: Env) extends LilaController(env):

  private val logger = lila.log("plan")

  def index(page: Int = 1) = Open:
    pageHit
    Reasonable(page):
      if HTTPRequest.isXhr(req) then
        Ok.snipAsync:
          env.plan.api.paginator(page).map(views.plan.topPatrons)
      else
        ctx.me.foldUse(indexAnon): me ?=>
          import lila.plan.PlanApi.SyncResult.*
          env.plan.api
            .sync(me)
            .flatMap:
              case ReloadUser => Redirect(routes.Plan.index())
              case Synced(Some(patron), None, None) =>
                env.user.repo.email(me).flatMap { email =>
                  renderIndex(email, patron.some)
                }
              case Synced(Some(patron), Some(stripeCus), _) => indexStripePatron(patron, stripeCus)
              case Synced(Some(_), _, Some(payPalSub)) => indexPayPalPatron(payPalSub)
              case _ => indexFreeUser

  def list = Open:
    ctx.me.foldUse(Redirect(routes.Plan.index()).toFuccess): me ?=>
      import lila.plan.PlanApi.SyncResult.*
      env.plan.api
        .sync(me)
        .flatMap:
          case ReloadUser => Redirect(routes.Plan.list)
          case Synced(Some(_), _, _) => indexFreeUser
          case _ => Redirect(routes.Plan.index())

  private def indexAnon(using Context) = renderIndex(email = none, patron = none)

  private def indexFreeUser(using ctx: Context, me: Me) =
    env.user.repo.email(me).flatMap { renderIndex(_, patron = none) }

  private def renderIndex(email: Option[EmailAddress], patron: Option[lila.plan.Patron])(using
      Context
  ): Fu[Result] =
    for
      recentIds <- env.plan.api.recentChargeUserIds
      _ <- env.user.lightUserApi.preloadMany(recentIds)
      bestScores <- env.plan.api.paginator(1)
      pricing <- env.plan.priceApi.pricingOrDefault(myCurrency)
      page <- renderPage:
        views.plan.index(
          stripePublicKey = env.plan.stripePublicKey,
          payPalPublicKey = env.plan.payPalPublicKey,
          email = email,
          patron = patron,
          recentIds = recentIds,
          bestScores = bestScores,
          pricing = pricing
        )
    yield Ok(page).withHeaders(crossOriginPolicy.unsafe*)

  private def indexStripePatron(patron: lila.plan.Patron, customer: StripeCustomer)(using
      ctx: Context,
      me: Me
  ) = for
    pricing <- env.plan.priceApi.pricingOrDefault(myCurrency)
    info <- env.plan.api.stripe.customerInfo(me, customer)
    gifts <- env.plan.api.giftsFrom(me)
    res <- info match
      case Some(info: CustomerInfo.Monthly) =>
        Ok.page(views.plan.indexStripe(me, info, env.plan.stripePublicKey, pricing, gifts))
          .map(_.withHeaders(crossOriginPolicy.unsafe*))
      case Some(CustomerInfo.OneTime(cus)) =>
        renderIndex(cus.email.map { EmailAddress(_) }, patron.some)
      case None =>
        env.user.repo
          .email(me)
          .flatMap:
            renderIndex(_, patron.some)
  yield res

  private def indexPayPalPatron(sub: PayPalSubscription)(using ctx: Context, me: Me) =
    Ok.async:
      env.plan.api.giftsFrom(me).map { views.plan.indexPayPal(me, sub, _) }
    .map:
        _.withHeaders(crossOriginPolicy.unsafe*)

  def features = Open:
    pageHit
    Ok.page(views.planPages.features)

  def switch = AuthBody { ctx ?=> me ?=>
    for
      pricing <- env.plan.priceApi.pricingOrDefault(myCurrency)
      _ <- bindForm(lila.plan.Switch.form(pricing))(
        _ => funit,
        data => env.plan.api.switch(me, data.money)
      )
    yield Redirect(routes.Plan.index())
  }

  private def myCurrency(using ctx: Context) =
    def ipCountry = env.security.geoIP(ctx.ip).flatMap(_.countryCode)
    env.plan.currencyApi.guessCurrency(get("currency"), ipCountry)

  def cancel = AuthBody { _ ?=> me ?=>
    env.plan.api.cancel(me).inject(Redirect(routes.Plan.index()))
  }

  def thanks = Auth { _ ?=> _ ?=>
    // wait for the payment data from stripe or paypal
    lila.common.LilaFuture.delay(2.seconds):
      for
        patron <- ctx.me.so { env.plan.api.userPatron(_) }
        customer <- patron.so(env.plan.api.stripe.patronCustomer)
        gift <- ctx.me.so { env.plan.api.recentGiftFrom(_) }
        page <- renderPage(views.planPages.thanks(patron, customer, gift))
      yield Ok(page)
  }

  def style = AuthBody { _ ?=> _ ?=>
    def redirect = Redirect(routes.Plan.index())
    bindForm(lila.plan.PlanForm.style)(
      _ => redirect,
      id =>
        for _ <- env.plan.api.setColorById(id)
        yield redirect
    )
  }

  def webhook = AnonBodyOf(parse.json): body =>
    if req.headers.hasHeader("PAYPAL-TRANSMISSION-SIG")
    then env.plan.webhook.payPal(body).inject(Ok("kthxbye"))
    else env.plan.webhook.stripe(body).inject(Ok("kthxbye"))

  import lila.plan.StripeClient.{ StripeException, CantUseException }
  def badStripeApiCall: PartialFunction[Throwable, Result] =
    case e @ CantUseException => BadRequest(jsonError(e.getMessage))
    case e: StripeException =>
      logger.error("Plan.stripeCheckout", e)
      BadRequest(jsonError("Stripe API call failed"))

  private def createStripeSession(
      checkout: PlanCheckout,
      customerId: StripeCustomerId,
      giftTo: Option[lila.user.User]
  )(using Context, Me) =
    env.plan.api.stripe
      .createSession(checkout, customerId, giftTo, routeUrl)
      .fold(badStripeApiCall, JsonOk)

  def switchStripePlan(money: Money)(using me: Me) =
    env.plan.api
      .switch(me, money)
      .inject(jsonOkResult)
      .recover(badStripeApiCall)

  def stripeCheckout = AuthBody { ctx ?=> me ?=>
    doStripeCheckout
  }

  def apiStripeCheckout = ScopedBody(_.Web.Mobile) { ctx ?=> me ?=>
    doStripeCheckout
  }

  def doStripeCheckout(using ctx: BodyContext[?], me: Me) =
    limit.planCheckout(ctx.ip, rateLimited):
      env.plan.priceApi
        .pricingOrDefault(myCurrency)
        .flatMap: pricing =>
          bindForm(env.plan.checkoutForm.form(pricing))(
            err =>
              logger.info(s"Plan.stripeCheckout 400: $err")
              BadRequest(jsonError(err.errors.map(_.message).mkString(", ")))
            ,
            data =>
              val checkout = data.fixFreq
              lila.mon.plan.intent("stripe", data.money.currency, data.coverFees).increment()
              for
                gifted <- checkout.giftTo.filterNot(ctx.is).so(env.user.repo.enabledById)
                customer <- env.plan.api.stripe.userCustomer(me)
                session <- customer match
                  case Some(customer) if checkout.freq == Freq.Onetime =>
                    createStripeSession(checkout, customer.id, gifted)
                  case Some(customer) if customer.firstSubscription.isDefined =>
                    switchStripePlan(checkout.money)
                  case _ =>
                    env.plan.api.stripe
                      .makeCustomer(me, checkout)
                      .flatMap(customer => createStripeSession(checkout, customer.id, gifted))
              yield session
          )

  def apiCurrencies = Anon:
    env.plan.priceApi.stripePricesAsJson(myCurrency).map(JsonStrOk)

  def updatePayment = AuthBody { ctx ?=> me ?=>
    limit.planCapture(ctx.ip, rateLimited):
      env.plan.api.stripe
        .userCustomer(me)
        .flatMap:
          _.flatMap(_.firstSubscription).map(_.copy(ip = ctx.ip.some)).so { sub =>
            env.plan.api.stripe
              .createPaymentUpdateSession(
                sub,
                NextUrls(
                  cancel = routeUrl(routes.Plan.index()),
                  success =
                    routeUrl(routes.Plan.updatePaymentCallback).map(_ + "?session={CHECKOUT_SESSION_ID}")
                )
              )
              .map(session => JsonOk(Json.obj("session" -> Json.obj("id" -> session.id.value))))
              .recover(badStripeApiCall)
          }
  }

  def updatePaymentCallback = AuthBody { ctx ?=> me ?=>
    get("session").so { session =>
      env.plan.api.stripe
        .userCustomer(me)
        .flatMap:
          _.flatMap(_.firstSubscription).so { sub =>
            env.plan.api.stripe.updatePaymentMethod(sub, session).inject(Redirect(routes.Plan.index()))
          }
    }
  }

  def payPalCheckout = AuthBody { ctx ?=> me ?=>
    limit.planCheckout(ctx.ip, rateLimited):
      env.plan.priceApi.pricingOrDefault(myCurrency).flatMap { pricing =>
        bindForm(env.plan.checkoutForm.form(pricing))(
          err =>
            logger.info(s"Plan.payPalCheckout 400: $err")
            BadRequest(jsonError(err.errors.map(_.message).mkString(", ")))
          ,
          data =>
            val checkout = data.fixFreq
            lila.mon.plan.intent("paypal", data.money.currency, data.coverFees).increment()
            if checkout.freq.renew then
              env.plan.api.payPal
                .createSubscription(checkout, me)
                .map: sub =>
                  JsonOk(Json.obj("subscription" -> Json.obj("id" -> sub.id.value)))
            else
              for
                gifted <- checkout.giftTo.filterNot(ctx.is(_)).so(env.user.repo.enabledById)
                // customer <- env.plan.api.userCustomer(me)
                order <- env.plan.api.payPal.createOrder(checkout, me, gifted)
              yield JsonOk(Json.obj("order" -> Json.obj("id" -> order.id.value)))
        )
      }
  }

  def payPalCapture(orderId: String) = Auth { ctx ?=> me ?=>
    limit.planCapture(ctx.ip, rateLimited):
      get("sub")
        .map(PayPalSubscriptionId.apply)
        .match
          case None => env.plan.api.payPal.captureOrder(PayPalOrderId(orderId), ctx.ip)
          case Some(subId) =>
            env.plan.api.payPal.captureSubscription(PayPalOrderId(orderId), subId, me, ctx.ip)
        .inject(jsonOkResult)
  }

  // deprecated
  def payPalIpn = AnonBody:
    bindForm(lila.plan.PlanForm.ipn)(
      err =>
        if err.errors("txn_type").nonEmpty then
          logger.debug(s"Plan.payPalIpn ignore txn_type = ${err.data.get("txn_type")}")
          Ok
        else
          logger.error(s"Plan.payPalIpn invalid data ${err.toString}")
          BadRequest
      ,
      ipn =>
        env.plan.api.payPal
          .onLegacyCharge(
            ipn,
            ip = req.ipAddress,
            key = get("key") | "N/A"
          )
          .inject(Ok)
    )

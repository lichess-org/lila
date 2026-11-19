package controllers

import play.api.mvc.*

import lila.app.*
import lila.recap.Recap.Availability
import lila.recap.ui.RecapUi

final class Recap(env: Env) extends LilaController(env):

  def home = Auth { _ ?=> me ?=>
    Redirect(routes.Recap.user(me.username))
  }

  def user(username: UserStr) = RecapPage(username) { ctx ?=> user => data =>
    negotiate(
      html = for
        checkout <- donationCheckout
        // PayPal's popup needs to talk back to this page, like on /patron
        page <- Ok.page(views.recap.home(data, user, checkout))
      yield page.withHeaders(crossOriginPolicy.unsafe*),
      json = data match
        case Availability.Available(data) => Ok(data).toFuccess
        case Availability.Queued(_) =>
          limit.recapAwaitConcurrency(user.id, rateLimited):
            env.recap.api.awaiter(user).map(Ok(_))
    )
  }

  private def donationCheckout(using ctx: Context): Fu[RecapUi.Checkout] =
    import RecapUi.Checkout.{ allMinutes, uptimeUsdPerMinute }
    import env.plan.priceApi
    for
      pricing <- priceApi.pricingOrDefault(env.plan.currencyApi.guessCurrency)
      local <- allMinutes.traverse(m => priceApi.localize(m * uptimeUsdPerMinute, pricing.currency))
    yield
      val (p, prices) = local.sequence.fold(
        priceApi.usdPricing -> allMinutes.map(_ * uptimeUsdPerMinute)
      )(pricing -> _.map(_.amount))
      val byMinutes = allMinutes.zip(prices).toMap
      RecapUi.Checkout(
        stripePublicKey = env.plan.stripePublicKey,
        payPalClientId = env.plan.payPalPublicKey,
        payPalLocale = lila.plan.PayPalClient.locale(ctx.lang),
        currency = p.currencyCode,
        stripe = p.stripeSupportsCurrency,
        payPal = p.payPalSupportsCurrency,
        price = byMinutes
      )

  private def RecapPage(
      username: UserStr
  )(f: Context ?=> UserModel => Availability => Fu[Result]): EssentialAction =
    Auth { ctx ?=> me ?=>
      if !lila.recap.Recap.availableForMe(env.mode) then
        Ok.page(views.recap.notAvailable(lila.recap.yearToRecap))
      else
        def proceed(user: lila.core.user.User) = for
          av <- env.recap.api.availability(user)
          res <- f(using ctx.updatePref(_.forceDarkBg))(user)(av)
        yield res
        if me.is(username) then proceed(me)
        else if isGranted(_.SeeInsight) || env.mode.notProd then Found(env.user.api.byId(username))(proceed)
        else Redirect(routes.Recap.home).toFuccess
    }

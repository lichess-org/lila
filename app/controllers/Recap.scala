package controllers

import play.api.mvc.*

import lila.app.*
import lila.plan.LichessJson.given
import lila.recap.Recap.Availability

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
            env.recap.api.awaiter(user)(env.plan.currencyApi.yearlyCostsForCtx).map(Ok(_))
    )
  }

  // picks the visitor's currency like /patron does: ?currency=, then their country, then their language
  private def donationCheckout(using ctx: Context): Fu[lila.recap.ui.RecapUi.Checkout] =
    val currencyApi = env.plan.currencyApi
    for
      pricing <- env.plan.priceApi.pricingOrDefault(currencyApi.guessCurrency(get("currency")))
      rate <- currencyApi.convert(lila.plan.Money(1, currencyApi.USD), pricing.currency).map(_.map(_.amount))
    yield
      // without an exchange rate, fall back to USD
      val (currency, usdRate, stripe, payPal) = rate match
        case Some(r) =>
          (
            pricing.currencyCode,
            r.toDouble,
            pricing.stripeSupportsCurrency,
            pricing.payPalSupportsCurrency
          )
        case None => ("USD", 1d, true, true)
      lila.recap.ui.RecapUi.Checkout(
        stripePublicKey = env.plan.stripePublicKey,
        payPalClientId = env.plan.payPalPublicKey,
        payPalLocale = lila.plan.PayPalClient.locale(ctx.lang),
        currency = currency,
        usdRate = usdRate,
        stripe = stripe,
        payPal = payPal
      )

  private def RecapPage(
      username: UserStr
  )(f: Context ?=> UserModel => Availability => Fu[Result]): EssentialAction =
    Auth { ctx ?=> me ?=>
      val availableAt = instantOf(lila.recap.yearToRecap, 12, 15, 0, 0)
      if env.mode.isProd && nowInstant.isBefore(availableAt) then
        Ok.page(views.recap.notAvailable(lila.recap.yearToRecap))
      else
        def proceed(user: lila.core.user.User) = for
          av <- env.recap.api.availability(user)(env.plan.currencyApi.yearlyCostsForCtx)
          res <- f(using ctx.updatePref(_.forceDarkBg))(user)(av)
        yield res
        if me.is(username) then proceed(me)
        else if isGranted(_.SeeInsight) || !env.mode.isProd then Found(env.user.api.byId(username))(proceed)
        else Redirect(routes.Recap.home).toFuccess
    }

package views.plan

import play.api.i18n.Lang

import lila.app.templating.Environment.{ *, given }
import lila.common.String.html.safeJsonValue

private lazy val ui      = lila.plan.ui.PlanUi(helpers)(netConfig.email)
private lazy val pagesUi = lila.plan.ui.PlanPages(helpers)(lila.fishnet.FishnetLimiter.maxPerDay)

def index(
    email: Option[EmailAddress],
    stripePublicKey: String,
    payPalPublicKey: String,
    patron: Option[lila.plan.Patron],
    recentIds: List[UserId],
    bestIds: List[UserId],
    pricing: lila.plan.PlanPricing
)(using ctx: PageContext) =
  val localeParam = lila.plan.PayPalClient.locale(ctx.lang).so { l => s"&locale=$l" }
  val pricingJson = safeJsonValue(lila.plan.PlanPricingApi.pricingWrites.writes(pricing))
  views.base.layout(
    title = trans.patron.becomePatron.txt(),
    moreCss = cssTag("plan"),
    moreJs = ctx.isAuth.option(
      frag(
        ui.stripeScript,
        frag(
          // gotta load the paypal SDK twice, for onetime and subscription :facepalm:
          // https://stackoverflow.com/questions/69024268/how-can-i-show-a-paypal-smart-subscription-button-and-a-paypal-smart-capture-but/69024269
          script(
            src := s"https://www.paypal.com/sdk/js?client-id=${payPalPublicKey}&currency=${pricing.currency}$localeParam",
            ui.namespaceAttr := "paypalOrder"
          ),
          script(
            src := s"https://www.paypal.com/sdk/js?client-id=${payPalPublicKey}&vault=true&intent=subscription&currency=${pricing.currency}$localeParam",
            ui.namespaceAttr := "paypalSubscription"
          )
        ),
        embedJsUnsafeLoadThen(s"""checkoutStart("$stripePublicKey", $pricingJson)""")(ctx.nonce)
      )
    ),
    modules = EsmInit("bits.checkout"),
    openGraph = OpenGraph(
      title = trans.patron.becomePatron.txt(),
      url = s"$netBaseUrl${routes.Plan.index.url}",
      description = trans.patron.freeChess.txt()
    ).some,
    csp = defaultCsp.withStripe.withPayPal.some
  )(ui.index(email, patron, recentIds, bestIds, pricing))

def indexPayPal(
    me: User,
    patron: lila.plan.Patron,
    subscription: lila.plan.PayPalSubscription,
    gifts: List[lila.plan.Charge.Gift]
)(using ctx: PageContext) =
  views.base.layout(
    title = trans.patron.thankYou.txt(),
    moreCss = cssTag("plan"),
    modules = EsmInit("bits.plan"),
    moreJs = embedJsUnsafeLoadThen("""plan.payPalStart()""")(ctx.nonce)
  )(ui.indexPayPal(me, patron, subscription, gifts))

def indexStripe(
    me: User,
    patron: lila.plan.Patron,
    info: lila.plan.CustomerInfo.Monthly,
    stripePublicKey: String,
    pricing: lila.plan.PlanPricing,
    gifts: List[lila.plan.Charge.Gift]
)(using ctx: PageContext) =
  views.base.layout(
    title = trans.patron.thankYou.txt(),
    moreCss = cssTag("plan"),
    modules = EsmInit("bits.plan"),
    moreJs = frag(
      ui.stripeScript,
      embedJsUnsafeLoadThen(s"""plan.stripeStart("$stripePublicKey")""")(ctx.nonce)
    ),
    csp = defaultCsp.withStripe.some
  )(ui.indexStripe(me, patron, info, pricing, gifts))

def features(using PageContext) =
  val title = "Lichess features"
  views.base.layout(
    title = title,
    moreCss = cssTag("feature"),
    openGraph = OpenGraph(
      title = title,
      url = s"$netBaseUrl${routes.Plan.features.url}",
      description = "All of Lichess features are free for all and forever. We do it for the chess!"
    ).some
  )(pagesUi.features)

def thanks(
    patron: Option[lila.plan.Patron],
    stripeCustomer: Option[lila.plan.StripeCustomer],
    gift: Option[lila.plan.Patron]
)(using PageContext) =
  views.base.layout(
    moreCss = cssTag("page"),
    title = trans.patron.thankYou.txt()
  )(pagesUi.thanks(patron, stripeCustomer, gift))

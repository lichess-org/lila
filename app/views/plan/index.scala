package views.html.plan

import play.api.i18n.Lang

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue

import controllers.routes

object index {

  import trans.patron._

  private[plan] val stripeScript = script(src := "https://js.stripe.com/v3/")

  def apply(
      email: Option[lila.common.EmailAddress],
      stripePublicKey: String,
      patron: Option[lila.plan.Patron],
      recentIds: List[String],
      bestIds: List[String],
      pricing: lila.plan.PlanPricing
  )(implicit ctx: Context) = {

    views.html.base.layout(
      title = becomePatron.txt(),
      moreCss = cssTag("plan"),
      moreJs = frag(
        stripeScript,
        jsModule("checkout"),
        embedJsUnsafeLoadThen(s"""checkoutStart("$stripePublicKey", ${safeJsonValue(
          lila.plan.PlanPricingApi.pricingWrites.writes(pricing)
        )})""")
      ),
      openGraph = lila.app.ui
        .OpenGraph(
          title = becomePatron.txt(),
          url = s"$netBaseUrl${routes.Plan.index.url}",
          description = freeChess.txt()
        )
        .some,
      csp = defaultCsp.withStripe.some
    ) {
      main(cls := "page-menu plan")(
        st.aside(cls := "page-menu__menu recent-patrons")(
          h2(newPatrons()),
          div(cls := "list")(
            recentIds.map { userId =>
              div(userIdLink(userId.some))
            }
          )
        ),
        div(cls := "page-menu__content box")(
          patron.ifTrue(ctx.me.??(_.isPatron)).map { p =>
            div(cls := "banner one_time_active")(
              iconTag(patronIconChar),
              div(
                h1(thankYou()),
                if (p.isLifetime) youHaveLifetime()
                else
                  p.expiresAt.map { expires =>
                    frag(
                      patronUntil(showDate(expires)),
                      br,
                      ifNotRenewed()
                    )
                  }
              ),
              iconTag(patronIconChar)
            )
          } getOrElse div(cls := "banner moto")(
            iconTag(patronIconChar),
            div(
              h1(freeChess()),
              p(noAdsNoSubs())
            ),
            iconTag(patronIconChar)
          ),
          div(cls := "box__pad")(
            div(cls := "wrapper")(
              div(cls := "text")(
                p(weAreNonProfit()),
                p(weRelyOnSupport())
              ),
              div(cls := "content")(
                div(
                  cls := "plan_checkout",
                  attr("data-email") := email.??(_.value),
                  attr("data-lifetime-amount") := pricing.lifetime.amount
                )(
                  raw(s"""
<form class="paypal_checkout onetime none" action="https://www.paypal.com/cgi-bin/webscr" method="post" target="_top">
${payPalFormSingle(pricing, "lichess.org one-time")}
</form>
<form class="paypal_checkout monthly none" action="https://www.paypal.com/cgi-bin/webscr" method="post" target="_top">
${payPalFormRecurring(pricing, "lichess.org monthly")}
</form>
<form class="paypal_checkout lifetime none" action="https://www.paypal.com/cgi-bin/webscr" method="post" target="_top">
${payPalFormSingle(pricing, "lichess.org lifetime")}
</form>"""),
                  st.group(cls := "radio buttons dest")(
                    div(
                      ctx.me match {
                        case None =>
                          a(href := s"${routes.Auth.login}?referrer=${routes.Plan.index}")("Login to donate")
                        case Some(me) =>
                          frag(
                            input(
                              tpe := "radio",
                              name := "dest",
                              id := "dest_me",
                              checked,
                              value := "me"
                            ),
                            label(`for` := "dest_me")("Donate as ", me.username)
                          )
                      }
                    ),
                    div(
                      input(
                        tpe := "radio",
                        name := "dest",
                        id := "dest_gift",
                        value := "gift"
                      ),
                      label(`for` := "dest_gift")("Gift Patron to a player")
                    )
                  ),
                  div(cls := "gift complete-parent none")(
                    st.input(
                      name := "giftUsername",
                      value := "",
                      cls := "user-autocomplete",
                      placeholder := trans.clas.lichessUsername.txt(),
                      autocomplete := "off",
                      dataTag := "span",
                      autofocus
                    )
                  ),
                  st.group(cls := "radio buttons freq")(
                    div(
                      st.title := singleDonation.txt(),
                      input(
                        tpe := "radio",
                        name := "freq",
                        id := "freq_onetime",
                        value := "onetime"
                      ),
                      label(`for` := "freq_onetime")(onetime())
                    ),
                    div(
                      st.title := recurringBilling.txt(),
                      input(
                        tpe := "radio",
                        name := "freq",
                        id := "freq_monthly",
                        checked,
                        value := "monthly"
                      ),
                      label(`for` := "freq_monthly")(monthly())
                    ),
                    div(
                      st.title := payLifetimeOnce.txt(pricing.lifetime.display),
                      input(
                        tpe := "radio",
                        name := "freq",
                        id := "freq_lifetime",
                        patron.exists(_.isLifetime) option disabled,
                        value := "lifetime",
                        cls := List("lifetime-check" -> patron.exists(_.isLifetime))
                      ),
                      label(`for` := "freq_lifetime")(lifetime())
                    )
                  ),
                  div(cls := "amount_choice")(
                    st.group(cls := "radio buttons amount")(
                      pricing.suggestions.map { money =>
                        val id = s"plan_${money.code}"
                        div(
                          input(
                            cls := money == pricing.default option "default",
                            tpe := "radio",
                            name := "plan",
                            st.id := id,
                            money == pricing.default option checked,
                            value := money.amount,
                            attr("data-amount") := money.amount
                          ),
                          label(`for` := id)(money.display)
                        )
                      },
                      div(cls := "other")(
                        input(tpe := "radio", name := "plan", id := "plan_other", value := "other"),
                        label(
                          `for` := "plan_other",
                          title := pleaseEnterAmountInX.txt(pricing.currencyCode),
                          attr("data-trans-other") := otherAmount.txt()
                        )(otherAmount())
                      )
                    )
                  ),
                  div(cls := "amount_fixed none")(
                    st.group(cls := "radio buttons amount")(
                      div(label(`for` := s"plan_${pricing.lifetime.code}")(pricing.lifetime.display))
                    )
                  ),
                  div(cls := "service")(
                    if (ctx.isAuth)
                      button(cls := "stripe button")(withCreditCard())
                    else
                      a(
                        cls := "stripe button",
                        href := s"${routes.Auth.login}?referrer=${routes.Plan.index}"
                      )(withCreditCard()),
                    button(cls := "paypal button")(withPaypal())
                  )
                )
              )
            ),
            p(id := "error")(),
            p(cls := "small_team")(weAreSmallTeam()),
            faq,
            div(cls := "best_patrons")(
              h2(celebratedPatrons()),
              div(cls := "list")(
                bestIds.map { userId =>
                  div(userIdLink(userId.some))
                }
              )
            )
          )
        )
      )
    }
  }

  private def payPalFormSingle(pricing: lila.plan.PlanPricing, itemName: String)(implicit ctx: Context) = s"""
  ${payPalForm(pricing, itemName)}
  <input type="hidden" name="cmd" value="_xclick">
  <input type="hidden" name="amount" class="amount" value="">
  <input type="hidden" name="button_subtype" value="services">
"""

  private def payPalFormRecurring(pricing: lila.plan.PlanPricing, itemName: String)(implicit ctx: Context) =
    s"""
  ${payPalForm(pricing, itemName)}
  <input type="hidden" name="cmd" value="_xclick-subscriptions">
  <input type="hidden" name="a3" class="amount" value="">
  <input type="hidden" name="p3" value="1">
  <input type="hidden" name="t3" value="M">
  <input type="hidden" name="src" value="1">
"""

  private def payPalForm(pricing: lila.plan.PlanPricing, itemName: String)(implicit ctx: Context) = s"""
  <input type="hidden" name="item_name" value="$itemName">
  <input type="hidden" name="custom" value="${~ctx.userId}">
  <input type="hidden" name="business" value="Q3H72BENTXL4G">
  <input type="hidden" name="no_note" value="1">
  <input type="hidden" name="no_shipping" value="1">
  <input type="hidden" name="rm" value="1">
  <input type="hidden" name="return" value="https://lichess.org/patron/thanks">
  <input type="hidden" name="cancel_return" value="https://lichess.org/patron">
  <input type="hidden" name="lc" value="${ctx.lang.locale}">
  <input type="hidden" name="currency_code" value="${pricing.currencyCode}">
"""

  private def faq(implicit lang: Lang) =
    div(cls := "faq")(
      dl(
        dt(whereMoneyGoes()),
        dd(
          serversAndDeveloper(userIdLink("thibault".some)),
          br,
          a(href := routes.Main.costs, targetBlank)(costBreakdown()),
          "."
        ),
        dt(officialNonProfit()),
        dd(
          a(
            href := "https://www.journal-officiel.gouv.fr/associations/detail-annonce/associations_b/20160025/818"
          )(actOfCreation()),
          "."
        )
      ),
      dl(
        dt(changeMonthlySupport()),
        dd(
          changeOrContact(a(href := routes.Main.contact, targetBlank)(contactSupport()))
        ),
        dt(otherMethods()),
        dd(
          "Lichess is registered with Benevity.",
          br,
          a(href := assetUrl("doc/iban_LICHESS_ORG_00022031601.pdf"), targetBlank)(bankTransfers()),
          ".",
          br,
          bitcoin(code("15ZA4bBki3uu3yR2ENC2WYa9baVGUZ8Cf8")),
          ".",
          br,
          "Please note that only the donation form above will grant the Patron status."
        )
      ),
      dl(
        dt(patronFeatures()),
        dd(
          noPatronFeatures(),
          br,
          a(href := routes.Plan.features, targetBlank)(featuresComparison()),
          "."
        )
      )
    )
}

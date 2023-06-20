package views.html.plan

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object index {

  import trans.patron._

  def apply(
      email: Option[lila.common.EmailAddress],
      stripePublicKey: String,
      patron: Option[lila.plan.Patron],
      recentIds: List[String],
      bestIds: List[String]
  )(implicit ctx: Context) = {

    views.html.base.layout(
      title = becomePatron.txt(),
      moreCss = cssTag("plan"),
      moreJs = frag(
        jsTag("checkout.js"),
        embedJsUnsafe(s"""lishogi.checkout("$stripePublicKey");""")
      ),
      openGraph = lila.app.ui
        .OpenGraph(
          title = becomePatron.txt(),
          url = s"$netBaseUrl${routes.Plan.index.url}",
          description = freeShogi.txt()
        )
        .some,
      csp = defaultCsp.withStripe.some,
      withHrefLangs = lila.i18n.LangList.All.some
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
              h1(freeShogi()),
              p(noAdsNoSubs())
            ),
            iconTag(patronIconChar)
          ),
          div(cls := "box__pad")(
            div(cls := "wrapper")(
              div(cls := "text")(
                p(if (ctx.isAuth) weRelyOnSupport() else donationSupport())
              ),
              div(cls := "content")(
                div(
                  cls                         := "plan_checkout",
                  attr("data-email")          := email.map(_.value),
                  attr("data-lifetime-usd")   := lila.plan.Cents.lifetime.usd.toString,
                  attr("data-lifetime-cents") := lila.plan.Cents.lifetime.value
                )(
                  raw(s"""
<form class="paypal_checkout onetime none" action="https://www.paypal.com/cgi-bin/webscr" method="post" target="_top">
  <input type="hidden" name="custom" value="${~ctx.userId}">
  <input type="hidden" name="amount" class="amount" value="">
  <input type="hidden" name="cmd" value="_xclick">
  <input type="hidden" name="business" value="RMMTS2XRNK4CS">
  <input type="hidden" name="item_name" value="lishogi.org one-time">
  <input type="hidden" name="button_subtype" value="services">
  <input type="hidden" name="no_note" value="1">
  <input type="hidden" name="no_shipping" value="1">
  <input type="hidden" name="rm" value="1">
  <input type="hidden" name="return" value="https://lishogi.org/patron/thanks">
  <input type="hidden" name="cancel_return" value="https://lishogi.org/patron">
  <input type="hidden" name="lc" value="US">
  <input type="hidden" name="currency_code" value="USD">
</form>
<form class="paypal_checkout monthly none" action="https://www.paypal.com/cgi-bin/webscr" method="post" target="_top">
  <input type="hidden" name="custom" value="${~ctx.userId}">
  <input type="hidden" name="a3" class="amount" value="">
  <input type="hidden" name="cmd" value="_xclick-subscriptions">
  <input type="hidden" name="business" value="RMMTS2XRNK4CS">
  <input type="hidden" name="item_name" value="lishogi.org monthly">
  <input type="hidden" name="no_note" value="1">
  <input type="hidden" name="no_shipping" value="1">
  <input type="hidden" name="rm" value="1">
  <input type="hidden" name="return" value="https://lishogi.org/patron/thanks">
  <input type="hidden" name="cancel_return" value="https://lishogi.org/patron">
  <input type="hidden" name="src" value="1">
  <input type="hidden" name="p3" value="1">
  <input type="hidden" name="t3" value="M">
  <input type="hidden" name="lc" value="US">
  <input type="hidden" name="currency_code" value="USD">
</form>
<form class="paypal_checkout lifetime none" action="https://www.paypal.com/cgi-bin/webscr" method="post" target="_top">
  <input type="hidden" name="custom" value="${~ctx.userId}">
  <input type="hidden" name="amount" class="amount" value="">
  <input type="hidden" name="cmd" value="_xclick">
  <input type="hidden" name="business" value="RMMTS2XRNK4CS">
  <input type="hidden" name="item_name" value="lishogi.org lifetime">
  <input type="hidden" name="button_subtype" value="services">
  <input type="hidden" name="no_note" value="1">
  <input type="hidden" name="no_shipping" value="1">
  <input type="hidden" name="rm" value="1">
  <input type="hidden" name="return" value="https://lishogi.org/patron/thanks">
  <input type="hidden" name="cancel_return" value="https://lishogi.org/patron">
  <input type="hidden" name="lc" value="US">
  <input type="hidden" name="currency_code" value="USD">
</form>"""),
                  patron.exists(_.isLifetime) option
                    p(style := "text-align:center;margin-bottom:1em")(makeExtraDonation()),
                  st.group(
                    cls := List(
                      "radio buttons freq" -> true,
                      "anon"               -> ctx.isAnon
                    )
                  )(
                    div(
                      st.title := payLifetimeOnce.txt(lila.plan.Cents.lifetime.usd),
                      cls      := List("lifetime-check" -> patron.exists(_.isLifetime)),
                      input(
                        tpe  := "radio",
                        name := "freq",
                        id   := "freq_lifetime",
                        patron.exists(_.isLifetime) option disabled,
                        value := "lifetime"
                      ),
                      label(`for` := "freq_lifetime")(lifetime())
                    ),
                    div(
                      st.title := recurringBilling.txt(),
                      input(
                        tpe  := "radio",
                        name := "freq",
                        id   := "freq_monthly",
                        checked,
                        value := "monthly"
                      ),
                      label(`for` := "freq_monthly")(monthly())
                    ),
                    div(
                      st.title := singleDonation.txt(),
                      input(
                        tpe  := "radio",
                        name := "freq",
                        id   := "freq_onetime",
                        checked,
                        value := "onetime"
                      ),
                      label(`for` := "freq_onetime")(onetime())
                    )
                  ),
                  div(cls := "amount_choice")(
                    st.group(cls := "radio buttons amount")(
                      lila.plan.StripePlan.defaultAmounts.map { cents =>
                        val id = s"plan_${cents.value}"
                        div(
                          input(
                            tpe   := "radio",
                            name  := "plan",
                            st.id := id,
                            cents.usd.value == 10 option checked,
                            value               := cents.value,
                            attr("data-usd")    := cents.usd.toString,
                            attr("data-amount") := cents.value
                          ),
                          label(`for` := id)(cents.usd.toString)
                        )
                      },
                      div(cls     := "other")(
                        input(tpe := "radio", name := "plan", id := "plan_other", value := "other"),
                        label(
                          `for`                    := "plan_other",
                          title                    := pleaseEnterAmount.txt(),
                          attr("data-trans-other") := otherAmount.txt()
                        )(otherAmount())
                      )
                    )
                  ),
                  div(cls := "amount_fixed none")(
                    st.group(cls := "radio buttons amount")(
                      div {
                        val cents = lila.plan.Cents.lifetime
                        label(`for` := s"plan_${cents.value}")(cents.usd.toString)
                      }
                    )
                  ),
                  div(cls := "service")(
                    button(
                      cls := List(
                        "paypal button" -> true,
                        "disabled"      -> ctx.isAnon
                      )
                    )(withPaypal())
                  ),
                  ctx.isAnon option div(cls := "confirm-policy")(
                    label(
                      frag(
                        input(
                          tpe := "checkbox",
                          cls := "regular-checkbox"
                        ),
                        trans.agreeWithX(
                          a(href := routes.Page.donations, target := "_blank")(
                            donationPolicy()
                          )
                        )
                      )
                    )
                  )
                )
              )
            ),
            p(id := "error")(),
            ctx.isAuth option p(cls := "small_team")(weAreSmallTeam()),
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

  private def faq(implicit ctx: Context) =
    div(cls := "faq")(
      ctx.isAuth option dl(
        dt(changeMonthlySupport()),
        dd(
          changeOrContact(a(href := routes.Main.contact, target := "_blank")(contactSupport()))
        )
      ),
      dl(
        dt(otherMethods()),
        dd(
          frag(trans.yes(), " - "),
          a(cls := "patreon", href := "https://www.patreon.com/lishogi", target := "_blank")(
            "Patreon"
          ),
          ".",
          ctx.isAuth option " Donations through Patreon will be handled manually within few days."
        )
      ),
      dl(
        dt(patronFeatures()),
        dd(
          if (ctx.isAuth) frag(noPatronFeatures(), br) else frag(trans.no(), ". "),
          a(href := routes.Plan.features, target := "_blank")(featuresComparison()),
          "."
        )
      )
    )
}

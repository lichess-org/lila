package views.html.plan

import controllers.routes
import java.util.Currency
import play.api.i18n.Lang

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.String.html.safeJsonValue

object index:

  import trans.patron.*

  private[plan] val stripeScript = script(src := "https://js.stripe.com/v3/")

  private val namespaceAttr = attr("data-namespace")

  def apply(
      email: Option[lila.common.EmailAddress],
      stripePublicKey: String,
      payPalPublicKey: String,
      patron: Option[lila.plan.Patron],
      recentIds: List[UserId],
      bestIds: List[UserId],
      pricing: lila.plan.PlanPricing
  )(using ctx: PageContext) =
    val localeParam = lila.plan.PayPalClient.locale(ctx.lang) so { l => s"&locale=$l" }
    views.html.base.layout(
      title = becomePatron.txt(),
      moreCss = cssTag("plan"),
      moreJs = ctx.isAuth option
        frag(
          stripeScript,
          frag(
            // gotta load the paypal SDK twice, for onetime and subscription :facepalm:
            // https://stackoverflow.com/questions/69024268/how-can-i-show-a-paypal-smart-subscription-button-and-a-paypal-smart-capture-but/69024269
            script(
              src := s"https://www.paypal.com/sdk/js?client-id=${payPalPublicKey}&currency=${pricing.currency}$localeParam",
              namespaceAttr := "paypalOrder"
            ),
            script(
              src := s"https://www.paypal.com/sdk/js?client-id=${payPalPublicKey}&vault=true&intent=subscription&currency=${pricing.currency}$localeParam",
              namespaceAttr := "paypalSubscription"
            )
          ),
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
      csp = defaultCsp.withStripe.withPayPal.some
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
          patron.ifTrue(ctx.me.so(_.isPatron)).map { p =>
            div(cls := "banner one_time_active")(
              iconTag(patronIconChar),
              div(
                h1(cls := "box__top")(thankYou()),
                if p.isLifetime then youHaveLifetime()
                else
                  p.expiresAt.map { expires =>
                    frag(
                      patronUntil(showDate(expires)),
                      br,
                      ifNotRenewedThenAccountWillRevert()
                    )
                  }
              ),
              iconTag(patronIconChar)
            )
          } getOrElse div(cls := "banner moto")(
            iconTag(patronIconChar),
            div(
              h1(cls := "box__top")(freeChess()),
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
                  cls                          := "plan_checkout",
                  attr("data-email")           := email.so(_.value),
                  attr("data-lifetime-amount") := pricing.lifetime.amount
                )(
                  ctx.me map { me =>
                    st.group(cls := "radio buttons dest")(
                      div(
                        input(
                          tpe  := "radio",
                          name := "dest",
                          id   := "dest_me",
                          checked,
                          value := "me"
                        ),
                        label(`for` := "dest_me")(donateAsX(me.username))
                      ),
                      div(
                        input(
                          tpe   := "radio",
                          name  := "dest",
                          id    := "dest_gift",
                          value := "gift"
                        ),
                        label(`for` := "dest_gift")(giftPatronWings())
                      )
                    )
                  },
                  div(cls := "gift complete-parent none")(
                    st.input(
                      name         := "giftUsername",
                      value        := "",
                      cls          := "user-autocomplete",
                      placeholder  := trans.clas.lichessUsername.txt(),
                      autocomplete := "off",
                      spellcheck   := false,
                      dataTag      := "span",
                      autofocus
                    )
                  ),
                  st.group(cls := "radio buttons freq")(
                    div(
                      st.title := singleDonation.txt(),
                      input(
                        tpe   := "radio",
                        name  := "freq",
                        id    := "freq_onetime",
                        value := "onetime"
                      ),
                      label(`for` := "freq_onetime")(onetime())
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
                      st.title := payLifetimeOnce.txt(pricing.lifetime.display),
                      input(
                        tpe  := "radio",
                        name := "freq",
                        id   := "freq_lifetime",
                        patron.exists(_.isLifetime) option disabled,
                        value := "lifetime",
                        cls   := List("lifetime-check" -> patron.exists(_.isLifetime))
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
                            cls   := money == pricing.default option "default",
                            tpe   := "radio",
                            name  := "plan",
                            st.id := id,
                            money == pricing.default option checked,
                            value               := money.amount,
                            attr("data-amount") := money.amount
                          ),
                          label(`for` := id)(money.display)
                        )
                      },
                      div(cls := "other")(
                        input(tpe := "radio", name := "plan", id := "plan_other", value := "other"),
                        label(
                          `for`                    := "plan_other",
                          title                    := pleaseEnterAmountInX.txt(pricing.currencyCode),
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
                    div(cls := "buttons")(
                      if ctx.isAuth then
                        frag(
                          button(cls := "stripe button")(donate()),
                          div(cls := "paypal paypal--order"),
                          div(cls := "paypal paypal--subscription"),
                          button(cls := "button disabled paypal--disabled")("PAYPAL")
                        )
                      else
                        a(
                          cls  := "button",
                          href := s"${routes.Auth.login}?referrer=${routes.Plan.index}"
                        )(logInToDonate())
                    ),
                    ctx.isAuth option div(cls := "other-choices")(
                      a(cls := "currency-toggle")(trans.patron.changeCurrency()),
                      div(cls := "links")(
                        a(cls := "stripe")("Google Pay"),
                        a(cls := "stripe")("Apple Pay")
                      )
                    ),
                    form(cls := "currency none", action := routes.Plan.list)(
                      select(name := "currency")(
                        lila.plan.CurrencyApi.currencyList.map { cur =>
                          option(
                            value := cur.getCurrencyCode,
                            pricing.currencyCode == cur.getCurrencyCode option selected
                          )(showCurrency(cur))
                        }
                      )
                    )
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

  private def showCurrency(cur: Currency)(using ctx: PageContext) =
    s"${cur.getSymbol(ctx.lang.locale)} ${cur.getDisplayName(ctx.lang.locale)}"

  private def faq(using Lang) =
    div(cls := "faq")(
      dl(
        dt(whereMoneyGoes()),
        dd(
          serversAndDeveloper(userIdLink(UserId("thibault").some)),
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
          lichessIsRegisteredWith(
            a(href := "https://causes.benevity.org/causes/250-5789375887401_bf01")("Benevity")
          ),
          br,
          views.html.site.contact.contactEmailLinkEmpty()(bankTransfers()),
          ".",
          br,
          strong(onlyDonationFromAbove())
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

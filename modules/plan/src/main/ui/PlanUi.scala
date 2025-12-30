package lila.plan
package ui

import java.util.Currency
import play.api.libs.json.Json
import scalalib.paginator.Paginator

import lila.ui.ScalatagsTemplate.{ *, given }
import lila.ui.*
import lila.core.LightUser
import lila.plan.PlanPricingApi.pricingWrites

final class PlanUi(helpers: Helpers)(style: PlanStyle, contactEmail: EmailAddress):
  import helpers.{ *, given }
  import trans.patron as trp

  private val stripeScript = script(src := "https://js.stripe.com/v3/")
  private val namespaceAttr = attr("data-namespace")
  private val dataForm = attr("data-form")

  def index(
      email: Option[EmailAddress],
      stripePublicKey: String,
      payPalPublicKey: String,
      patron: Option[Patron],
      recentIds: List[UserId],
      bestScores: Paginator[LightUser],
      pricing: PlanPricing
  )(using ctx: Context) =
    val localeParam = lila.plan.PayPalClient.locale(ctx.lang).so { l => s"&locale=$l" }
    Page(trans.patron.becomePatron.txt())
      .css("bits.plan")
      .i18n(_.patron)
      .append:
        ctx.isAuth.so:
          frag(
            stripeScript,
            pricing.payPalSupportsCurrency.option:
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
              )
          )
      .js:
        ctx.isAuth.option:
          esmInitObj("bits.checkout", "stripePublicKey" -> stripePublicKey, "pricing" -> pricing)
      .js(infiniteScrollEsmInit)
      .graph(
        title = trans.patron.becomePatron.txt(),
        url = routeUrl(routes.Plan.index()),
        description = trans.patron.freeChess.txt()
      )
      .csp(paymentCsp):
        main(cls := "page-menu plan")(
          st.aside(cls := "page-menu__menu recent-patrons")(
            h2(trp.newPatrons()),
            div(cls := "list")(
              recentIds.map: userId =>
                div(userIdLink(userId.some))
            )
          ),
          div(cls := "page-menu__content box")(
            patron.match
              case Some(patron) =>
                frag(
                  div(cls := "banner one_time_active")(
                    iconTag(patronIconChar),
                    div(
                      h1(cls := "box__top")(trp.thankYou()),
                      if ctx.me.exists(_.plan.lifetime) then trp.youHaveLifetime()
                      else
                        patron.expiresAt.map: expires =>
                          frag(
                            trp.patronUntil(showDate(expires)),
                            br,
                            trp.ifNotRenewedThenAccountWillRevert()
                          )
                    ),
                    iconTag(patronIconChar)
                  ),
                  style.selector.map(_(cls := "box__pad"))
                )
              case None =>
                div(cls := "banner moto")(
                  iconTag(patronIconChar),
                  div(
                    h1(cls := "box__top")(trp.freeChess()),
                    p(trp.noAdsNoSubs())
                  ),
                  iconTag(patronIconChar)
                ),
            div(cls := "box__pad")(
              div(cls := "wrapper")(
                div(cls := "text")(
                  p(trp.weAreNonProfit()),
                  p(trp.weRelyOnSupport()),
                  p("Click donate to view payment options based on your device and currency.")
                ),
                div(cls := "content")(
                  div(
                    cls := "plan_checkout",
                    attr("data-email") := email.so(_.value),
                    attr("data-lifetime-amount") := pricing.lifetime.amount
                  )(
                    ctx.me.map { me =>
                      st.group(cls := "radio buttons dest")(
                        div(
                          input(
                            tpe := "radio",
                            name := "dest",
                            id := "dest_me",
                            checked,
                            value := "me"
                          ),
                          label(`for` := "dest_me")(trp.donateAsX(me.username))
                        ),
                        div(
                          input(
                            tpe := "radio",
                            name := "dest",
                            id := "dest_gift",
                            value := "gift"
                          ),
                          label(`for` := "dest_gift")(trp.giftPatronWings())
                        )
                      )
                    },
                    ctx.isAuth.option(
                      frag(
                        div(cls := "gift complete-parent none")(
                          st.input(
                            name := "giftUsername",
                            value := "",
                            cls := "user-autocomplete",
                            placeholder := trans.clas.lichessUsername.txt(),
                            autocomplete := "off",
                            spellcheck := false,
                            dataTag := "span",
                            autofocus
                          )
                        ),
                        st.group(cls := "radio buttons freq")(
                          div(
                            st.title := trp.singleDonation.txt(),
                            input(
                              tpe := "radio",
                              name := "freq",
                              id := "freq_onetime",
                              value := "onetime"
                            ),
                            label(`for` := "freq_onetime")(trp.onetime())
                          ),
                          div(
                            st.title := trp.recurringBilling.txt(),
                            input(
                              tpe := "radio",
                              name := "freq",
                              id := "freq_monthly",
                              checked,
                              value := "monthly"
                            ),
                            label(`for` := "freq_monthly")(trp.monthly())
                          ),
                          div(
                            st.title := trp.payLifetimeOnce.txt(pricing.lifetime.display),
                            input(
                              tpe := "radio",
                              name := "freq",
                              id := "freq_lifetime",
                              ctx.me.exists(_.plan.lifetime).option(disabled),
                              value := "lifetime",
                              cls := List("lifetime-check" -> ctx.me.exists(_.plan.lifetime))
                            ),
                            label(`for` := "freq_lifetime")(trp.lifetime())
                          )
                        ),
                        div(cls := "amount_choice")(
                          st.group(cls := "radio buttons amount")(
                            pricing.suggestions.map { money =>
                              val id = s"plan_${money.code}"
                              div(
                                input(
                                  cls := (money == pricing.default).option("default"),
                                  tpe := "radio",
                                  name := "plan",
                                  st.id := id,
                                  (money == pricing.default).option(checked),
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
                                title := trp.pleaseEnterAmountInX.txt(pricing.currencyCode),
                                attr("data-trans-other") := trp.otherAmount.txt()
                              )(trp.otherAmount())
                            )
                          )
                        ),
                        div(cls := "amount_fixed none")(
                          st.group(cls := "radio buttons amount")(
                            div(label(`for` := s"plan_${pricing.lifetime.code}")(pricing.lifetime.display))
                          )
                        )
                      )
                    ),
                    ctx.isAuth.option(
                      div(cls := "cover-fees")(
                        input(tpe := "checkbox", id := "cover-fees", cls := "cover-fees-checkbox"),
                        label(`for` := "cover-fees"):
                          val rawFee = pricing.feeFixed.amount.max(pricing.default.amount * pricing.feeRate)
                          trp.coverFees.txt(Money(rawFee, pricing.currency).display)
                      )
                    ),
                    div(cls := "service")(
                      div(cls := "buttons")(
                        if ctx.isAuth then
                          frag(
                            pricing.stripeSupportsCurrency.option:
                              button(cls := "stripe button")(trp.donate())
                            ,
                            pricing.payPalSupportsCurrency.option:
                              frag(
                                div(cls := "paypal paypal--order"),
                                div(cls := "paypal paypal--subscription"),
                                button(cls := "button disabled paypal--disabled")("PAYPAL")
                              )
                          )
                        else
                          a(
                            cls := "button",
                            href := s"${routes.Auth.login}?referrer=${routes.Plan.index}"
                          )(trp.logInToDonate())
                      ),
                      ctx.isAuth.option(
                        div(cls := "other-choices")(
                          a(cls := "currency-toggle")(trans.patron.changeCurrency())
                        )
                      ),
                      form(cls := "currency none", action := routes.Plan.list)(
                        select(name := "currency")(
                          CurrencyApi.currencyList.map: cur =>
                            st.option(
                              value := cur.getCurrencyCode,
                              (pricing.currencyCode == cur.getCurrencyCode).option(selected)
                            )(showCurrency(cur))
                        )
                      )
                    )
                  )
                )
              ),
              p(id := "error")(),
              p(cls := "small_team")(trp.weAreSmallTeam()),
              faq,
              div(cls := "best_patrons")(
                h2(trp.celebratedPatrons()),
                topPatrons(bestScores)
              )
            )
          )
        )

  def topPatrons(users: Paginator[LightUser])(using Context) =
    div(cls := "list infinite-scroll")(
      users.currentPageResults.map: u =>
        div(cls := "paginated")(lightUserLink(u)),
      pagerNext(users, np => s"${routes.Plan.index(np).url}")
    )

  private def showCurrency(cur: Currency)(using ctx: Context) =
    s"${cur.getSymbol(ctx.lang.locale)} ${cur.getDisplayName(ctx.lang.locale)}"

  private def faq(using Translate) =
    div(cls := "faq")(
      dl(
        dt(trp.whereMoneyGoes()),
        dd(
          trp.serversAndDeveloper(userIdLink(UserId("thibault").some)),
          br,
          a(href := routes.Main.costs, targetBlank)(trp.costBreakdown()),
          "."
        ),
        dt(trp.officialNonProfit()),
        dd(
          a(
            href := "https://www.journal-officiel.gouv.fr/associations/detail-annonce/associations_b/20160025/818"
          )(trp.actOfCreation()),
          "."
        )
      ),
      dl(
        dt(trp.changeMonthlySupport()),
        dd(
          trp.changeOrContact(a(href := routes.Main.contact, targetBlank)(trp.contactSupport()))
        ),
        dt(trp.otherMethods()),
        dd(
          trp.lichessIsRegisteredWith(
            a(href := "https://causes.benevity.org/causes/250-5789375887401_bf01")("Benevity")
          ),
          br,
          bits.contactEmailLinkEmpty(contactEmail.value)(trp.bankTransfers()),
          ".",
          br,
          strong(trp.onlyDonationFromAbove())
        )
      ),
      dl(
        dt(trp.patronFeatures()),
        dd(
          trp.noPatronFeatures(),
          br,
          a(href := routes.Plan.features, targetBlank)(trp.featuresComparison()),
          "."
        )
      )
    )

  def indexPayPal(
      me: User,
      subscription: PayPalSubscription,
      gifts: List[Charge.Gift]
  )(using Context) =
    Page(trans.patron.thankYou.txt())
      .css("bits.plan")
      .js(esmInit("bits.plan")):
        main(cls := "box box-pad plan")(
          boxTop(
            h1(
              userLink(me),
              " • ",
              if me.plan.lifetime then strong(trp.lifetimePatron())
              else trp.patronForMonths(me.plan.months)
            )
          ),
          style.selector,
          table(cls := "all")(
            tbody(
              tr(
                th(trp.currentStatus()),
                td(
                  trp.youSupportWith(strong(subscription.capturedMoney.display)),
                  span(cls := "thanks")(trp.tyvm())
                )
              ),
              tr(
                th(trp.nextPayment()),
                td(
                  trp.youWillBeChargedXOnY(
                    strong(subscription.capturedMoney.display),
                    showDate(subscription.nextChargeAt)
                  ),
                  br,
                  a(href := s"${routes.Plan.list}?freq=onetime")(trp.makeAdditionalDonation())
                )
              ),
              tr(
                th(trp.update()),
                td(cls := "change") {
                  val cancelButton = a(dataForm := "cancel")(trp.cancelSupport())
                  frag(
                    cancelButton,
                    postForm(cls := "cancel", action := routes.Plan.cancel)(
                      p(trp.stopPayments()),
                      submitButton(cls := "button button-red")(trp.noLongerSupport()),
                      a(dataForm := "cancel")(trans.site.cancel())
                    )
                  )
                }
              ),
              tr(
                th("Gifts"),
                td(
                  a(href := s"${routes.Plan.list}?dest=gift")(trp.giftPatronWings()),
                  gifts.nonEmpty.option(
                    table(cls := "slist gifts")(
                      tbody(
                        gifts.map { gift =>
                          tr(
                            td(userIdLink(gift.to.some)),
                            td(momentFromNow(gift.date))
                          )
                        }
                      )
                    )
                  )
                )
              ),
              tr(
                th,
                td(a(href := routes.Plan.list)(trp.viewOthers()))
              )
            )
          )
        )

  def indexStripe(
      me: User,
      info: CustomerInfo.Monthly,
      stripePublicKey: String,
      pricing: PlanPricing,
      gifts: List[Charge.Gift]
  )(using ctx: Context) =
    Page(trans.patron.thankYou.txt())
      .css("bits.plan")
      .append(stripeScript)
      .js(esmInitObj("bits.plan", Json.obj("stripePublicKey" -> stripePublicKey)))
      .csp(paymentCsp):
        main(cls := "box box-pad plan")(
          boxTop(
            h1(
              userLink(me),
              " • ",
              if me.plan.lifetime then strong(trp.lifetimePatron())
              else trp.patronForMonths(me.plan.months)
            )
          ),
          style.selector,
          table(cls := "all")(
            tbody(
              tr(
                th(trp.currentStatus()),
                td(
                  trp.youSupportWith(strong(info.subscription.item.price.money.display)),
                  span(cls := "thanks")(trp.tyvm())
                )
              ),
              tr(
                th(trp.nextPayment()),
                td(
                  trp.youWillBeChargedXOnY(
                    strong(info.nextInvoice.money.display),
                    showDate(info.nextInvoice.dateTime)
                  ),
                  br,
                  a(href := s"${routes.Plan.list}?freq=onetime")(trp.makeAdditionalDonation())
                )
              ),
              tr(
                th(trp.update()),
                td(cls := "change") {
                  val cancelButton = a(dataForm := "cancel")(trp.cancelSupport())
                  frag(
                    if pricing.currency != info.subscription.item.price.currency then cancelButton
                    else
                      trp.xOrY(
                        a(dataForm := "switch")(
                          trp.changeMonthlyAmount(info.subscription.item.price.money.display)
                        ),
                        cancelButton
                      )
                    ,
                    postForm(cls := "switch", action := routes.Plan.switch)(
                      p(trp.decideHowMuch()),
                      strong(pricing.currency.getSymbol(ctx.lang.locale), nbsp),
                      input(
                        tpe := "number",
                        min := pricing.min.amount,
                        max := pricing.max.amount,
                        step := {
                          if CurrencyApi.zeroDecimalCurrencies contains pricing.currency then "1"
                          else "0.01"
                        },
                        name := "amount",
                        value := {
                          (info.subscription.item.price.currency == pricing.currency)
                            .so(info.subscription.item.price.money.amount.toString)
                        }
                      ),
                      submitButton(cls := "button")(trans.site.apply()),
                      a(dataForm := "switch")(trans.site.cancel())
                    ),
                    postForm(cls := "cancel", action := routes.Plan.cancel)(
                      p(trp.stopPaymentsPayPal()),
                      submitButton(cls := "button button-red")(trp.noLongerSupport()),
                      a(dataForm := "cancel")(trans.site.cancel())
                    )
                  )
                }
              ),
              tr(
                th(trp.paymentDetails()),
                td(
                  info.paymentMethod.flatMap(_.card).map { m =>
                    frag(
                      m.brand.toUpperCase,
                      " - ",
                      "*" * 12,
                      m.last4,
                      " - ",
                      m.exp_month,
                      "/",
                      m.exp_year,
                      br
                    )
                  },
                  a(cls := "update-payment-method")(trp.updatePaymentMethod())
                )
              ),
              tr(
                th("Gifts"),
                td(
                  a(href := s"${routes.Plan.list}?dest=gift")(trp.giftPatronWings()),
                  gifts.nonEmpty.option(
                    table(cls := "slist gifts")(
                      tbody(
                        gifts.map { gift =>
                          tr(
                            td(userIdLink(gift.to.some)),
                            td(momentFromNow(gift.date))
                          )
                        }
                      )
                    )
                  )
                )
              ),
              tr(
                th("Stripe"),
                td:
                  a(href := "https://billing.stripe.com/p/login/fZefZ2dCK9zq7Ty6oo"):
                    trp.stripeManageSub()
              ),
              tr(
                th,
                td(a(href := routes.Plan.list)(trp.viewOthers()))
              )
            )
          )
        )

  private def paymentCsp: Update[ContentSecurityPolicy] = csp =>
    csp.copy(
      connectSrc = "https://*.stripe.com" :: "https://*.paypal.com" :: csp.connectSrc,
      scriptSrc = "https://*.stripe.com" :: "https://*.paypal.com" :: csp.scriptSrc,
      frameSrc = "https://*.stripe.com" :: "https://*.paypal.com" :: csp.frameSrc
    )

package views.html.plan

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.plan.CurrencyApi.zeroDecimalCurrencies

object indexStripe:

  import trans.patron.*

  private val dataForm = attr("data-form")

  def apply(
      me: lila.user.User,
      patron: lila.plan.Patron,
      info: lila.plan.CustomerInfo.Monthly,
      stripePublicKey: String,
      pricing: lila.plan.PlanPricing,
      gifts: List[lila.plan.Charge.Gift]
  )(using ctx: PageContext) =
    views.html.base.layout(
      title = thankYou.txt(),
      moreCss = cssTag("plan"),
      moreJs = frag(
        index.stripeScript,
        jsModule("plan"),
        embedJsUnsafeLoadThen(s"""plan.stripeStart("$stripePublicKey")""")
      ),
      csp = defaultCsp.withStripe.some
    ) {
      main(cls := "box box-pad plan")(
        boxTop(
          h1(
            userLink(me),
            " • ",
            if patron.isLifetime then strong(lifetimePatron())
            else patronForMonths(me.plan.months)
          )
        ),
        table(cls := "all")(
          tbody(
            tr(
              th(currentStatus()),
              td(
                youSupportWith(strong(info.subscription.item.price.money.display)),
                span(cls := "thanks")(tyvm())
              )
            ),
            tr(
              th(nextPayment()),
              td(
                youWillBeChargedXOnY(
                  strong(info.nextInvoice.money.display),
                  showDate(info.nextInvoice.dateTime)
                ),
                br,
                a(href := s"${routes.Plan.list}?freq=onetime")(makeAdditionalDonation())
              )
            ),
            tr(
              th(update()),
              td(cls := "change") {
                val cancelButton = a(dataForm := "cancel")(cancelSupport())
                frag(
                  if pricing.currency != info.subscription.item.price.currency then cancelButton
                  else
                    xOrY(
                      a(dataForm := "switch")(
                        changeMonthlyAmount(info.subscription.item.price.money.display)
                      ),
                      cancelButton
                    )
                  ,
                  postForm(cls := "switch", action := routes.Plan.switch)(
                    p(decideHowMuch()),
                    strong(pricing.currency.getSymbol(ctx.lang.locale), nbsp),
                    input(
                      tpe := "number",
                      min := pricing.min.amount,
                      max := pricing.max.amount,
                      step := {
                        if zeroDecimalCurrencies contains pricing.currency then "1"
                        else "0.01"
                      },
                      name := "amount",
                      value := {
                        (info.subscription.item.price.currency == pricing.currency) so
                          info.subscription.item.price.money.amount.toString
                      }
                    ),
                    submitButton(cls := "button")(trans.apply()),
                    a(dataForm := "switch")(trans.cancel())
                  ),
                  postForm(cls := "cancel", action := routes.Plan.cancel)(
                    p(stopPaymentsPayPal()),
                    submitButton(cls := "button button-red")(noLongerSupport()),
                    a(dataForm := "cancel")(trans.cancel())
                  )
                )
              }
            ),
            tr(
              th("Payment details"),
              td(
                info.paymentMethod.flatMap(_.card) map { m =>
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
                a(cls := "update-payment-method")("Update payment method")
              )
            ),
            tr(
              th("Gifts"),
              td(
                a(href := s"${routes.Plan.list}?dest=gift")(giftPatronWings()),
                gifts.nonEmpty option
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
            ),
            tr(
              th,
              td(a(href := routes.Plan.list)(viewOthers()))
            )
          )
        )
      )
    }

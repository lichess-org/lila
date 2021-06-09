package views.html.plan

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.plan.CurrencyApi.zeroDecimalCurrencies

import controllers.routes

object indexStripe {

  import trans.patron._

  private val dataForm = attr("data-form")

  def apply(
      me: lila.user.User,
      patron: lila.plan.Patron,
      info: lila.plan.MonthlyCustomerInfo,
      stripePublicKey: String,
      pricing: lila.plan.PlanPricing,
      gifts: List[lila.plan.Charge.Gift]
  )(implicit
      ctx: Context
  ) =
    views.html.base.layout(
      title = thankYou.txt(),
      moreCss = cssTag("plan"),
      moreJs = frag(
        index.stripeScript,
        jsModule("plan"),
        embedJsUnsafeLoadThen(s"""planStart("$stripePublicKey")""")
      ),
      csp = defaultCsp.withStripe.some
    ) {
      main(cls := "box box-pad plan")(
        h1(
          userLink(me),
          " • ",
          if (patron.isLifetime) strong(lifetimePatron())
          else patronForMonths(me.plan.months)
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
                a(href := s"${routes.Plan.list}#onetime")(makeAdditionalDonation())
              )
            ),
            tr(
              th(update()),
              td(cls := "change") {
                val cancelButton = a(dataForm := "cancel")(cancelSupport())
                frag(
                  if (pricing.currency != info.subscription.item.price.currency) cancelButton
                  else
                    xOrY(
                      a(dataForm := "switch")(
                        changeMonthlyAmount(info.subscription.item.price.money.display)
                      ),
                      cancelButton
                    ),
                  postForm(cls := "switch", action := routes.Plan.switch)(
                    p(decideHowMuch()),
                    strong(pricing.currency.getSymbol(ctx.lang.locale), nbsp),
                    input(
                      tpe := "number",
                      min := pricing.min.amount,
                      max := pricing.max.amount,
                      step := {
                        if (zeroDecimalCurrencies contains pricing.currency) "1"
                        else "0.01"
                      },
                      name := "amount",
                      value := {
                        (info.subscription.item.price.currency == pricing.currency) ??
                          info.subscription.item.price.money.amount.toString
                      }
                    ),
                    submitButton(cls := "button")(trans.apply()),
                    a(dataForm := "switch")(trans.cancel())
                  ),
                  postForm(cls := "cancel", action := routes.Plan.cancel)(
                    p(stopPayments()),
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
                a(href := s"${routes.Plan.list}#gift")(giftPatronWings()),
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
}

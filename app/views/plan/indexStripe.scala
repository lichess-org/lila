package views.html.plan

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object indexStripe {

  import trans.patron._

  private val dataForm = attr("data-form")

  def apply(me: lila.user.User, patron: lila.plan.Patron, info: lila.plan.MonthlyCustomerInfo)(implicit
      ctx: Context
  ) =
    views.html.base.layout(
      title = thankYou.txt(),
      moreCss = cssTag("plan"),
      moreJs = jsTag("plan.js")
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
                youSupportWith(strong(info.subscription.price.usd.toString)),
                span(cls := "thanks")(tyvm())
              )
            ),
            tr(
              th(nextPayment()),
              td(
                youWillBeChargedXOnY(
                  strong(info.nextInvoice.usd.toString),
                  showDate(info.nextInvoice.dateTime)
                ),
                br,
                a(href := s"${routes.Plan.list}#onetime")(makeAdditionalDonation())
              )
            ),
            tr(
              th(update()),
              td(cls := "change")(
                xOrY(
                  a(dataForm := "switch")(
                    changeMonthlyAmount(info.subscription.price.usd.toString)
                  ),
                  a(dataForm := "cancel")(cancelSupport())
                ),
                postForm(cls := "switch", action := routes.Plan.switch)(
                  p(decideHowMuch()),
                  "USD $ ",
                  input(
                    tpe := "number",
                    min := 1,
                    max := 100000,
                    step := "0.01",
                    name := "usd",
                    value := info.subscription.price.usd.toString
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
            ),
            tr(
              th(paymentHistory()),
              td(
                table(cls := "slist payments")(
                  thead(
                    tr(
                      th,
                      th("ID"),
                      th(date()),
                      th(amount())
                    )
                  ),
                  tbody(
                    info.pastInvoices.map { in =>
                      tr(
                        td(in.paid option span(dataIcon := "E", cls := "is-green text")(paid())),
                        td(cls := "id")(in.id),
                        td(showDate(in.dateTime)),
                        td(in.usd.toString)
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

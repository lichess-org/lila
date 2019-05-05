package views.html.plan

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object indexStripe {

  private val dataForm = attr("data-form")

  def apply(me: lila.user.User, patron: lila.plan.Patron, info: lila.plan.MonthlyCustomerInfo)(implicit ctx: Context) =
    views.html.base.layout(
      title = "Thank you for your support!",
      moreCss = cssTag("plan"),
      moreJs = jsTag("plan.js")
    ) {
        main(cls := "box box-pad plan")(
          h1(
            userLink(me), " • ",
            if (patron.isLifetime) strong("Lifetime Lichess Patron")
            else frag("Lichess Patron for ", pluralize("month", me.plan.months))
          ),
          table(cls := "all")(
            tbody(
              tr(
                th("Current status"),
                td(
                  "You support lichess.org with ", strong(info.subscription.plan.usd.toString), " per month.",
                  span(cls := "thanks")("Thank you very much for your help. You rock!")
                )
              ),
              tr(
                th("Next payment"),
                td(
                  "You will be charged ", strong(info.nextInvoice.usd.toString), " on ", showDate(info.nextInvoice.dateTime), ".",
                  br,
                  a(href := s"${routes.Plan.list()}#onetime")("Make an additional donation now")
                )
              ),
              tr(
                th("Update"),
                td(cls := "change")(
                  a(dataForm := "switch")("Change the monthly amount (", info.subscription.plan.usd.toString, ")"),
                  " or ", a(dataForm := "cancel")("cancel your support"),
                  form(cls := "switch", action := routes.Plan.switch, method := "POST")(
                    p("Decide what Lichess is worth to you:"),
                    "USD $ ",
                    input(tpe := "number", min := 1, max := 100000, step := "0.01", name := "usd", value := info.subscription.plan.usd.toString),
                    button(tpe := "submit", cls := "button")(trans.apply()),
                    a(dataForm := "switch")("Nevermind")
                  ),
                  form(cls := "cancel", action := routes.Plan.cancel, method := "POST")(
                    p("Withdraw your credit card and stop payments:"),
                    button(tpe := "submit", cls := "button button-red")("No longer support Lichess"),
                    a(dataForm := "cancel")("Nevermind :)")
                  )
                )
              ),
              tr(
                th("Payment history"),
                td(
                  table(cls := "slist payments")(
                    thead(
                      tr(
                        th,
                        th("ID"),
                        th("Date"),
                        th("Amount")
                      )
                    ),
                    tbody(
                      info.pastInvoices.map { in =>
                        tr(
                          td(in.paid option span(dataIcon := "E", cls := "is-green text")("Paid")),
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
                td(a(href := routes.Plan.list)("View other Lichess Patrons"))
              )
            )
          )
        )
      }
}

package views.html.plan

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object indexStripe {

  private val dataForm = attr("data-form")

  def apply(me: lidraughts.user.User, patron: lidraughts.plan.Patron, info: lidraughts.plan.MonthlyCustomerInfo)(implicit ctx: Context) =
    views.html.base.layout(
      title = "Thank you for your support!",
      moreCss = cssTag("plan"),
      moreJs = jsTag("plan.js")
    ) {
        main(cls := "box box-pad plan")(
          h1(
            userLink(me), " • ",
            if (patron.isLifetime) strong("Lifetime Lidraughts Patron")
            else frag("Lidraughts Patron for ", pluralize("month", me.plan.months))
          ),
          table(cls := "all")(
            tbody(
              tr(
                th("Current status"),
                td(
                  "You support lidraughts.org with ", strong(info.subscription.plan.usd.toString), " per month.",
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
                  postForm(cls := "switch", action := routes.Plan.switch)(
                    p("Decide what Lidraughts is worth to you:"),
                    "EUR $ ",
                    input(tpe := "number", min := 1, max := 100000, step := "0.01", name := "usd", value := info.subscription.plan.usd.toString),
                    submitButton(cls := "button")(trans.apply()),
                    a(dataForm := "switch")("Nevermind")
                  ),
                  postForm(cls := "cancel", action := routes.Plan.cancel)(
                    p("Withdraw your credit card and stop payments:"),
                    submitButton(cls := "button button-red")("No longer support Lidraughts"),
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
                td(a(href := routes.Plan.list)("View other Lidraughts Patrons"))
              )
            )
          )
        )
      }
}

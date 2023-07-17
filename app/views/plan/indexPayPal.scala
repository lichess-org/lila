package views.html.plan

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

object indexPayPal:

  import trans.patron.*

  private val dataForm = attr("data-form")

  def apply(
      me: lila.user.User,
      patron: lila.plan.Patron,
      subscription: lila.plan.PayPalSubscription,
      gifts: List[lila.plan.Charge.Gift]
  )(using PageContext) =
    views.html.base.layout(
      title = thankYou.txt(),
      moreCss = cssTag("plan"),
      moreJs = frag(jsModule("plan"), embedJsUnsafeLoadThen("""plan.payPalStart()"""))
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
                youSupportWith(strong(subscription.capturedMoney.display)),
                span(cls := "thanks")(tyvm())
              )
            ),
            tr(
              th(nextPayment()),
              td(
                youWillBeChargedXOnY(
                  strong(subscription.capturedMoney.display),
                  showDate(subscription.nextChargeAt)
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
                  cancelButton,
                  postForm(cls := "cancel", action := routes.Plan.cancel)(
                    p(stopPayments()),
                    submitButton(cls := "button button-red")(noLongerSupport()),
                    a(dataForm := "cancel")(trans.cancel())
                  )
                )
              }
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

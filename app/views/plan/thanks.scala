package views.html.plan

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object thanks {

  def apply(patron: Option[lila.plan.Patron], customer: Option[lila.plan.StripeCustomer])(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = cssTag("page"),
      title = "Thank you for your support!"
    ) {
        main(cls := "page-small page box box-pad")(

          h1(cls := "text", dataIcon := patronIconChar)("Thank you for your support!"),

          div(cls := "body")(
            p("Thank you for helping us build Lichess. ", strong("You rock!")),
            p(
              "Your transaction has been completed, ",
              "and a receipt for your donation has been emailed to you."
            ),
            patron.map { pat =>
              if (pat.payPal.??(_.renew)) p(
                "You now have a permanent Patron account.", br,
                ctx.me.map { me =>
                  frag("Check out your ", a(href := routes.User.show(me.username))("profile page"), "!")
                }
              )
              else {
                if (customer.??(_.renew)) frag(
                  p(
                    "Note that your ", a(href := routes.Plan.index)("Patron page"),
                    " only shows invoices for your monthly subscription."
                  ),
                  p("But worry not, we received your donation! Thanks again!")
                )
                else frag(
                  if (pat.isLifetime) p(
                    "You are now a lifetime Lichess Patron!", br,
                    ctx.me.map { me =>
                      frag("Check out your ", a(href := routes.User.show(me.username))("profile page"), ".")
                    }
                  )
                  else frag(
                    p(
                      "You are now a Lichess Patron for one month!", br,
                      ctx.me.map { me =>
                        frag("Check out your ", a(href := routes.User.show(me.username))("profile page"), ".")
                      }
                    ),
                    p(
                      "In one month, you will ", strong("not"), " be charged again, ",
                      "and your Lichess account will be downgraded to free."
                    ),
                    p(
                      "To get a permanent Patron account, please consider making a ",
                      a(href := routes.Plan.index)("monthly donation"), "."
                    )
                  )
                )
              }
            },
            p("Success! ", a(href := routes.Lobby.home)("Return to Lichess homepage"), ".")
          )
        )
      }
}

package views.html.plan

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

import controllers.routes

object thanks:

  import trans.patron.*

  def apply(
      patron: Option[lila.plan.Patron],
      stripeCustomer: Option[lila.plan.StripeCustomer],
      gift: Option[lila.plan.Patron]
  )(using
      ctx: PageContext
  ) =
    views.html.base.layout(
      moreCss = cssTag("page"),
      title = thankYou.txt()
    ) {
      main(cls := "page-small page box box-pad")(
        boxTop(h1(cls := "text", dataIcon := patronIconChar)(thankYou())),
        div(cls := "body")(
          p(tyvm()),
          p(transactionCompleted()),
          (gift, patron) match
            case (Some(gift), _) =>
              p(
                userIdLink(gift.userId.some),
                " ",
                if gift.isLifetime then "is now a lifetime Lichess Patron"
                else "is now a Lichess Patron for one month",
                ", thanks to you!"
              )
            case (_, Some(pat)) =>
              if pat.payPal.exists(_.renew) ||
                pat.payPalCheckout.exists(_.renew) ||
                stripeCustomer.exists(_.renew)
              then
                ctx.me map { me =>
                  p(
                    permanentPatron(),
                    br,
                    a(href := routes.User.show(me.username))(checkOutProfile())
                  )
                }
              else
                frag(
                  if pat.isLifetime then
                    p(
                      nowLifetime(),
                      br,
                      ctx.me.map { me =>
                        a(href := routes.User.show(me.username))(checkOutProfile())
                      }
                    )
                  else
                    frag(
                      p(
                        nowOneMonth(),
                        br,
                        ctx.me.map { me =>
                          a(href := routes.User.show(me.username))(checkOutProfile())
                        }
                      ),
                      p(downgradeNextMonth())
                    )
                )
            case _ => emptyFrag
          ,
          br,
          br,
          br,
          br,
          br,
          br,
          a(href := s"${routes.Plan.list}?dest=gift")(makeAdditionalDonation())
        )
      )
    }

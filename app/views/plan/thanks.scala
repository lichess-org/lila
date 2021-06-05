package views.html.plan

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object thanks {

  import trans.patron._

  def apply(
      patron: Option[lila.plan.Patron],
      customer: Option[lila.plan.StripeCustomer],
      gift: Option[lila.plan.Patron]
  )(implicit
      ctx: Context
  ) =
    views.html.base.layout(
      moreCss = cssTag("page"),
      title = thankYou.txt()
    ) {
      main(cls := "page-small page box box-pad")(
        h1(cls := "text", dataIcon := patronIconChar)(thankYou()),
        div(cls := "body")(
          p(tyvm()),
          p(transactionCompleted()),
          (gift, patron) match {
            case (Some(gift), _) =>
              p(
                userIdLink(gift.userId.some),
                " ",
                if (gift.isLifetime) "is now a lifetime Lichess Patron"
                else "is now a Lichess Patron for one month",
                ", thanks to you!"
              )
            case (_, Some(pat)) =>
              if (pat.payPal.exists(_.renew) || customer.exists(_.renew)) ctx.me.fold(emptyFrag) { me =>
                p(
                  permanentPatron(),
                  br,
                  a(href := routes.User.show(me.username))(checkOutProfile())
                )
              }
              else {
                frag(
                  if (pat.isLifetime)
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
              }
            case _ => emptyFrag
          },
          br,
          br,
          br,
          br,
          br,
          br,
          a(href := s"${routes.Plan.list}#gift")(makeAdditionalDonation())
        )
      )
    }
}

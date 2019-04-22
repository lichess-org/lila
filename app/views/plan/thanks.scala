package views.html.plan

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.pref.PrefCateg

import controllers.routes

object thanks {

  def apply(patron: Option[lidraughts.plan.Patron], customer: Option[lidraughts.plan.StripeCustomer])(implicit ctx: Context) =
views.html.base.layout(
moreCss = cssTag("page"),
title = "Thank you for your support!") {
main( cls := "page-small page box box-pad")(

  h1( cls := "text", dataIcon := patronIconChar)("Thank you for your support!"),

  div( cls := "body")(
    p(
      "Thank you for helping us build Lidraughts. ", strong("You rock!")
    ),
    p(
      "Your transaction has been completed, ",
      "and a receipt for your donation has been emailed to you."
    ),
    patron.map { p =>
    if (p.payPal.??(_.renew)) p(
      "You now have a permanent Patron account.", br,
      ctx.me.map { me =>
      frag("Check out your ", a( href := routes.User.show(me.username))("profile page"), "!")
      }
      ) else {
    if (customer.??(_.renew)) p(
      "Note that your ", a( href := routes.Plan.index)("Patron page"), 
      " only shows invoices for your monthly subscription."
    ),
    p(
      But worry not, we received your donation! Thanks again!
    </p>
    } else {
    @if(p.isLifetime) {
    p(
      You are now a lifetime Lidraughts Patron!br( />
      @ctx.me.map { me =>
      Check out your a( href="@routes.User.show(me.username)">profile page</a>.
      }
    </p>
    } else {
    p(
      You are now a Lidraughts Patron for one month!br( />
      @ctx.me.map { me =>
      Check out your a( href="@routes.User.show(me.username)">profile page</a>.
      }
    </p>
    p(
      In one month, you will strong(not</strong> be charged again,
      and your Lidraughts account will be downgraded to free.
    </p>
    p(
      To get a permanent Patron account, please consider making a
      a( href="@routes.Plan.index">monthly donation</a>.
    </p>
    }
    }
    }
    }
    p(
      Success! a( href="@routes.Lobby.home">Return to Lidraughts homepage</a>.
    </p>
  </div>
</main>
}.toHtml

package views.html.plan

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.pref.PrefCateg

import controllers.routes

object index {

  def apply(
    email: Option[lila.common.EmailAddress],
    stripePublicKey: String,
    patron: Option[lila.plan.Patron],
    recentIds: List[String],
    bestIds: List[String]
  )(implicit ctx: Context) = {

    val title = "Become a Patron of lichess.org"

    views.html.base.layout(
      title = title,
      moreCss = cssTag("plan"),
      moreJs = frag(
        script(src := "https://checkout.stripe.com/checkout.js"),
        jsTag("checkout.js"),
        embedJsUnsafe(s"""lichess.checkout("$stripePublicKey");""")
      ),
      openGraph = lila.app.ui.OpenGraph(
        title = title,
        url = s"$netBaseUrl${routes.Plan.index.url}",
        description = "Free chess for everyone, forever!"
      ).some,
      csp = defaultCsp.withStripe.some
    ) {
        main(cls := "page-menu plan")(
          st.aside(cls := "page-menu__menu recent-patrons")(
            h2("New Patrons"),
            div(cls := "list")(
              recentIds.map { userId =>
                div(userIdLink(userId.some))
              }
            )
          ),
          div(cls := "page-menu__content box")(
            patron.ifTrue(ctx.me.??(_.isPatron)).map { p =>
              div(cls := "banner one_time_active")(
                iconTag(patronIconChar),
                div(
                  h1("Thank you for your donation!"),
                  if (p.isLifetime) frag(
                    "You have a ", strong("Lifetime Patron"), " account. That's pretty awesome!"
                  )
                  else p.expiresAt.map { expires =>
                    frag(
                      "You have a Patron account until ", showDate(expires), ".", br,
                      "If not renewed, you will then be downgraded to free."
                    )
                  }
                ),
                iconTag(patronIconChar)
              )
            } getOrElse div(cls := "banner moto")(
              iconTag(patronIconChar),
              div(
                h1("Free chess for everyone, forever!"),
                p("No ads, no subscriptions; but open source and passion.")
              ),
              iconTag(patronIconChar)
            ),
            div(cls := "box__pad")(
              div(cls := "wrapper")(
                div(cls := "text")(
                  p(
                    "We are a non‑profit association because we believe in a free, ",
                    "world-class chess experience for anyone, anywhere."
                  ),
                  p(
                    "We rely on support from people like you to make it possible. ",
                    "If you've gotten something out of Lichess, please take a second to pitch in!"
                  )
                ),
                div(cls := "content")(

                  div(
                    cls := "plan_checkout",
                    attr("data-email") := email.map(_.value),
                    attr("data-lifetime-usd") := lila.plan.Cents.lifetime.usd.toString,
                    attr("data-lifetime-cents") := lila.plan.Cents.lifetime.value
                  )(
                      raw(s"""
<form class="stripe_checkout none" action="${routes.Plan.charge}" method="POST">
  <input type="hidden" class="token" name="token" />
  <input type="hidden" class="email" name="email" />
  <input type="hidden" class="amount" name="amount" />
  <input type="hidden" class="freq" name="freq" />
</form>
<form class="paypal_checkout onetime none" action="https://www.paypal.com/cgi-bin/webscr" method="post" target="_top">
  <input type="hidden" name="custom" value="${~ctx.userId}">
  <input type="hidden" name="amount" class="amount" value="">
  <input type="hidden" name="cmd" value="_xclick">
  <input type="hidden" name="business" value="Q3H72BENTXL4G">
  <input type="hidden" name="item_name" value="lichess.org one-time">
  <input type="hidden" name="button_subtype" value="services">
  <input type="hidden" name="no_note" value="1">
  <input type="hidden" name="no_shipping" value="1">
  <input type="hidden" name="rm" value="1">
  <input type="hidden" name="return" value="https://lichess.org/patron/thanks">
  <input type="hidden" name="cancel_return" value="https://lichess.org/patron">
  <input type="hidden" name="lc" value="US">
  <input type="hidden" name="currency_code" value="USD">
</form>
<form class="paypal_checkout monthly none" action="https://www.paypal.com/cgi-bin/webscr" method="post" target="_top">
  <input type="hidden" name="custom" value="${~ctx.userId}">
  <input type="hidden" name="a3" class="amount" value="">
  <input type="hidden" name="cmd" value="_xclick-subscriptions">
  <input type="hidden" name="business" value="Q3H72BENTXL4G">
  <input type="hidden" name="item_name" value="lichess.org monthly">
  <input type="hidden" name="no_note" value="1">
  <input type="hidden" name="no_shipping" value="1">
  <input type="hidden" name="rm" value="1">
  <input type="hidden" name="return" value="https://lichess.org/patron/thanks">
  <input type="hidden" name="cancel_return" value="https://lichess.org/patron">
  <input type="hidden" name="src" value="1">
  <input type="hidden" name="p3" value="1">
  <input type="hidden" name="t3" value="M">
  <input type="hidden" name="lc" value="US">
  <input type="hidden" name="currency_code" value="USD">
</form>
<form class="paypal_checkout lifetime none" action="https://www.paypal.com/cgi-bin/webscr" method="post" target="_top">
  <input type="hidden" name="custom" value="${~ctx.userId}">
  <input type="hidden" name="amount" class="amount" value="">
  <input type="hidden" name="cmd" value="_xclick">
  <input type="hidden" name="business" value="Q3H72BENTXL4G">
  <input type="hidden" name="item_name" value="lichess.org lifetime">
  <input type="hidden" name="button_subtype" value="services">
  <input type="hidden" name="no_note" value="1">
  <input type="hidden" name="no_shipping" value="1">
  <input type="hidden" name="rm" value="1">
  <input type="hidden" name="return" value="https://lichess.org/patron/thanks">
  <input type="hidden" name="cancel_return" value="https://lichess.org/patron">
  <input type="hidden" name="lc" value="US">
  <input type="hidden" name="currency_code" value="USD">
</form>"""),

                      patron.exists(_.isLifetime) option
                        p(style := "text-align:center;margin-bottom:1em")("Make an extra donation?"),

                      st.group(cls := "radio buttons freq")(
                        div(
                          st.title := s"Pay ${lila.plan.Cents.lifetime.usd} once. Be a Lichess Patron forever!",
                          cls := List("lifetime-check" -> patron.exists(_.isLifetime)),
                          input(tpe := "radio", name := "freq", id := "freq_lifetime", patron.exists(_.isLifetime) option disabled, value := "lifetime"),
                          label(`for` := "freq_lifetime")("Lifetime")
                        ),
                        div(
                          st.title := "Recurring billing, renewing your Patron Wings every month.",
                          input(tpe := "radio", name := "freq", id := "freq_monthly", checked, value := "monthly"),
                          label(`for` := "freq_monthly")("Monthly")
                        ),
                        div(
                          st.title := "A single donation that grants you the Patron Wings for one month.",
                          input(tpe := "radio", name := "freq", id := "freq_onetime", checked, value := "onetime"),
                          label(`for` := "freq_onetime")("One-time")
                        )
                      ),
                      div(cls := "amount_choice")(
                        st.group(cls := "radio buttons amount")(
                          lila.plan.StripePlan.defaultAmounts.map { cents =>
                            val id = s"plan_${cents.value}"
                            div(
                              input(tpe := "radio", name := "plan",
                                st.id := id,
                                cents.usd.value == 10 option checked,
                                value := cents.value,
                                attr("data-usd") := cents.usd.toString,
                                attr("data-amount") := cents.value),
                              label(`for` := id)(cents.usd.toString)
                            )
                          },
                          div(cls := "other")(
                            input(tpe := "radio", name := "plan",
                              id := "plan_other",
                              value := "other"),
                            label(`for` := "plan_other")("Other")
                          )
                        )
                      ),
                      div(cls := "amount_fixed none")(
                        st.group(cls := "radio buttons amount")(
                          div {
                            val cents = lila.plan.Cents.lifetime
                            label(`for` := s"plan_${cents.value}")(cents.usd.toString)
                          }
                        )
                      ),
                      div(cls := "service")(
                        button(cls := "stripe button")("Credit Card"),
                        button(cls := "paypal button")("PayPal")
                      )
                    )
                )
              ),
              p(cls := "small_team")(
                "We are a small team, so your support makes a huge difference!"
              ),
              faq,
              div(cls := "best_patrons")(
                h2("The celebrated Patrons who make Lichess possible"),
                div(cls := "list")(
                  bestIds.map { userId =>
                    div(userIdLink(userId.some))
                  }
                )
              )
            )
          )
        )
      }
  }

  private def faq(implicit ctx: Context) = div(cls := "faq")(
    dl(
      dt("Where does the money go?"),
      dd(
        "First of all, powerful servers.", br,
        "Then we pay a full-time developer: ", userIdLink("thibault".some), ", the founder of Lichess.", br,
        "See the ", a(href := "/costs", target := "_blank")("detailed cost breakdown.")
      ),
      dt("Is Lichess an official non-profit?"),
      dd(
        "Yes, here's the ", a(href := "http://www.journal-officiel.gouv.fr/association/index.php?ACTION=Rechercher&WHAT=lichess.org")(
          "act of creation (FR)"
        ), "."
      )
    ),
    dl(
      dt("Can I change/cancel my monthly support?"),
      dd(
        "Yes, at any time, from this page.", br,
        "Or you can ", a(href := routes.Main.contact, target := "_blank")("contact Lichess support"), "."
      ),
      dt("Other methods of donation?"),
      dd(
        "We also accept ", a(href := staticUrl("doc/iban_LICHESS_ORG_00022031601.pdf"), target := "_blank")(
          "bank transfers"
        ), ".", br,
        "And here's our bitcoin address: ", code("15ZA4bBki3uu3yR2ENC2WYa9baVGUZ8Cf8")
      )
    ),
    dl(
      dt("Are some features reserved to Patrons?"),
      dd(
        a(href := routes.Plan.features, target := "_blank")("No"), ", because ",
        "Lichess is entirely free, forever, and for everyone. That's a promise. ",
        "But Patrons get bragging rights with a cool new profile icon.", br,
        "See the ", a(href := routes.Plan.features, target := "_blank")("detailed features comparison"), "."
      )
    )
  )
}

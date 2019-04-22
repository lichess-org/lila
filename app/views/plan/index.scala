package views.html.plan

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.pref.PrefCateg

import controllers.routes

object index {

  def apply(
    email: Option[lidraughts.common.EmailAddress],
    stripePublicKey: String,
    patron: Option[lidraughts.plan.Patron],
    recentIds: List[String],
    bestIds: List[String]
  )(implicit ctx: Context) = {

    val title = "Become a Patron of lidraughts.org"

    views.html.base.layout(
      title = title,
      moreCss = cssTag("plan"),
      moreJs = frag(
        script(src := "https://checkout.stripe.com/checkout.js"),
        jsTag("checkout.js"),
        embedJsUnsafe(s"""lidraughts.checkout("$stripePublicKey");""")
      ),
      openGraph = lidraughts.app.ui.OpenGraph(
        title = title,
        url = s"$netBaseUrl${routes.Plan.index.url}",
        description = "Free draughts for everyone, forever!"
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
                      "You have a Patron account until ", showDate(expires), br,
                      "If not renewed, you will then be downgraded to free."
                    )
                  }
                ),
                iconTag(patronIconChar)
              )
            } getOrElse div(cls := "banner moto")(
              iconTag(patronIconChar),
              div(
                h1("Free draughts for everyone, forever!"),
                p("No ads, no subscriptions; but open source and passion.")
              ),
              iconTag(patronIconChar)
            ),
            div(cls := "box__pad")(
              div(cls := "wrapper")(
                div(cls := "text")(
                  p(
                    "We are a small team of volunteers who believe in a free, ",
                    "world-class draughts experience for anyone, anywhere."
                  ),
                  p(
                    "We rely on support from people like you to make it possible. ",
                    "If you've gotten something out of Lidraughts, please take a second to pitch in!"
                  )
                ),
                div(cls := "content")(

                  div(
                    cls := "plan_checkout",
                    attr("data-email") := email.map(_.value),
                    attr("data-lifetime-usd") := lidraughts.plan.Cents.lifetime.usd.toString,
                    attr("data-lifetime-cents") := lidraughts.plan.Cents.lifetime.value
                  )(
                      raw(s"""
<form class="paypal_checkout onetime none" action="https://www.paypal.com/cgi-bin/webscr" method="post" target="_top">
  <input type="hidden" name="custom" value="${ctx.userId}">
  <input type="hidden" name="amount" class="amount" value="">
  <input type="hidden" name="cmd" value="_xclick">
  <input type="hidden" name="business" value="EWNKNKK58PMS6">
  <input type="hidden" name="item_name" value="lidraughts.org one-time">
  <input type="hidden" name="button_subtype" value="services">
  <input type="hidden" name="no_note" value="1">
  <input type="hidden" name="no_shipping" value="1">
  <input type="hidden" name="rm" value="1">
  <input type="hidden" name="return" value="$netBaseUrl/patron/thanks">
  <input type="hidden" name="cancel_return" value="$netBaseUrl/patron">
  <input type="hidden" name="lc" value="US">
  <input type="hidden" name="currency_code" value="EUR">
</form>
<form class="paypal_checkout monthly none" action="https://www.paypal.com/cgi-bin/webscr" method="post" target="_top">
  <input type="hidden" name="custom" value="${ctx.userId}">
  <input type="hidden" name="a3" class="amount" value="">
  <input type="hidden" name="cmd" value="_xclick-subscriptions">
  <input type="hidden" name="business" value="EWNKNKK58PMS6">
  <input type="hidden" name="item_name" value="lidraughts.org monthly">
  <input type="hidden" name="no_note" value="1">
  <input type="hidden" name="no_shipping" value="1">
  <input type="hidden" name="rm" value="1">
  <input type="hidden" name="return" value="$netBaseUrl/patron/thanks">
  <input type="hidden" name="cancel_return" value="$netBaseUrl/patron">
  <input type="hidden" name="src" value="1">
  <input type="hidden" name="p3" value="1">
  <input type="hidden" name="t3" value="M">
  <input type="hidden" name="lc" value="US">
  <input type="hidden" name="currency_code" value="EUR">
</form>
<form class="paypal_checkout lifetime none" action="https://www.paypal.com/cgi-bin/webscr" method="post" target="_top">
  <input type="hidden" name="custom" value="${ctx.userId}">
  <input type="hidden" name="amount" class="amount" value="">
  <input type="hidden" name="cmd" value="_xclick">
  <input type="hidden" name="business" value="EWNKNKK58PMS6">
  <input type="hidden" name="item_name" value="lidraughts.org lifetime">
  <input type="hidden" name="button_subtype" value="services">
  <input type="hidden" name="no_note" value="1">
  <input type="hidden" name="no_shipping" value="1">
  <input type="hidden" name="rm" value="1">
  <input type="hidden" name="return" value="$netBaseUrl/patron/thanks">
  <input type="hidden" name="cancel_return" value="$netBaseUrl/patron">
  <input type="hidden" name="lc" value="US">
  <input type="hidden" name="currency_code" value="EUR">
</form>"""),

                      patron.exists(_.isLifetime) option
                        p(style := "text-align:center;margin-bottom:1em")("Make an extra donation?"),

                      st.group(cls := "radio buttons freq")(
                        div(
                          st.title := s"Pay ${lidraughts.plan.Cents.lifetime.usd} once. Be a Lidraughts Patron forever!",
                          cls := List("lifetime-check" -> patron.exists(_.isLifetime)),
                          input(tpe := "radio", name := "freq", id := "freq_lifetime", patron.exists(_.isLifetime) option disabled, value := "lifetime"),
                          label(`for` := "freq_lifetime")("Lifetime")
                        ),
                        div(
                          st.title := "Recurring billing, renewing your Patron Wings every month.",
                          input(tpe := "radio", name := "freq", id := "freq_monthly", value := "monthly"),
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
                          lidraughts.plan.StripePlan.defaultAmounts.map { cents =>
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
                            val cents = lidraughts.plan.Cents.lifetime
                            label(`for` := s"plan_${cents.value}")(cents.usd.toString)
                          }
                        )
                      ),
                      div(cls := "service")(
                        // button(cls := "stripe button")("Credit Card"),
                        button(cls := "paypal button")("Donate with PayPal")
                      )
                    )
                )
              ),
              p(cls := "small_team")(
                "We are a small team, so your support makes a huge difference!"
              ),
              faq,
              div(cls := "best_patrons")(
                h2("The celebrated Patrons who make Lidraughts possible"),
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
        "The servers that run lidraughts and the Scan engine analysis. ",
        "Other operating costs such as mailgun and hosting.", br,
        "Possibly we can have titled tournaments with real money prizes at some point in the future!"
      )
    ),
    dl(
      dt("Can I change/cancel my monthly support?"),
      dd(
        "Yes, at any time. Login to your PayPal account and go to your automatic payments overview.", br,
        "Or you can ", a(href := routes.Page.contact, target := "_blank")("contact Lidraughts support"), "."
      ),
      dt("Other methods of donation?"),
      dd(
        "We also accept bank transfers. Please send an e-mail to ",
        contactEmailLink,
        " for details."
      )
    ),
    dl(
      dt("Are some features reserved to Patrons?"),
      dd(
        a(href := routes.Plan.features, target := "_blank")("No"), ", because ",
        "Lidraughts is entirely free, forever, and for everyone. That's a promise.",
        "But Patrons get bragging rights with a cool new profile icon.", br,
        "See the ", a(href := routes.Plan.features, target := "_blank")("detailed features comparison"), "."
      )
    )
  )
}

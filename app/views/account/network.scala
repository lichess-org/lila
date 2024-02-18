package views.html
package account

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

import controllers.routes

object network:

  def apply(cfRouting: Option[Boolean])(using ctx: PageContext) =
    account.layout(
      title = "Network",
      active = "network"
    ):
      val usingCloudflare = cfRouting.getOrElse(ctx.pref.isUsingAltSocket)
      div(cls := "box box-pad")(
        h1(cls := "box__top")("Network"),
        br,
        if usingCloudflare then
          frag(
            flashMessage("warning")("You are currently using Content Delivery Network (CDN) routing."),
            p("This feature is experimental but may improve reliability in some regions.")
          )
        else
          p("If you have frequent disconnects, Content Delivery Network (CDN) routing may improve things.")
        ,
        br,
        st.section(a(href := "#routing")(h2(id := "routing")("Network Routing")))(
          st.group(cls := "radio"):
            List(("Use direct routing", false), ("Use CDN routing", true)) map: (key, value) =>
              div(
                a(value != usingCloudflare option (href := routes.Account.network(value.some)))(
                  label(value == usingCloudflare option (cls := "active-soft"))(key)
                )
              )
        ),
        br,
        br,
        cfRouting.nonEmpty option p(cls := "saved text", dataIcon := licon.Checkmark)(
          trans.preferences.yourPreferencesHaveBeenSaved()
        )
      )

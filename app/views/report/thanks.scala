package views.html.report

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object thanks {

  def apply(userId: String, blocked: Boolean)(implicit ctx: Context) = {

    val title = "Thanks for the report"

    views.html.base.layout(title = title, moreJs = jsTag("misc.thanks-report")) {
      main(cls := "page-small box box-pad")(
        h1(title),
        p("The moderators will review it very soon, and take appropriate action."),
        br,
        br,
        !blocked option p(
          "In the meantime, you can block this user: ",
          submitButton(
            attr("data-action") := routes.Relation.block(userId),
            cls                 := "report-block button",
            st.title            := trans.block.txt()
          )(
            span(cls := "text", dataIcon := "k")("Block ", usernameOrId(userId))
          )
        ),
        br,
        br,
        p(
          a(href := routes.Lobby.home)("Return to Lishogi homepage")
        )
      )

    }
  }
}

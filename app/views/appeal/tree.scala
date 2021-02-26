package views.html
package appeal

import controllers.routes
import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.report.Report.Inquiry
import lila.user.User

object tree {

  import trans.contact.doNotMessageModerators
  import views.html.base.navTree._

  private def cleanMenu(implicit ctx: Context): Branch =
    Branch(
      "root",
      "Your account is not marked or restricted. You're all good!",
      List(
        Leaf(
          "clean-other-account",
          "I want to appeal for another account",
          frag(
            p(
              "Sorry we don't take appeals from other accounts. The appeal should come from nowhere else, but the concerned account."
            )
          )
        ),
        Leaf(
          "clean-warning",
          "I want to discuss a warning I received",
          frag(
            p(
              "Please note that warnings are only warnings, and that your account has not been restricted currently.",
              br,
              "If you still want to file an appeal, use the following form:"
            ),
            newAppeal
          )
        ),
        Leaf(
          "clean-other-issue",
          "I have another issue to discuss",
          p(
            "This channel of communication is for appealing moderation related issues.",
            br,
            "Please use ",
            a(href := routes.Main.contact)("the contact page"),
            " or ",
            a(href := "https://discordapp.com/invite/pvHanhg")("our discord server"),
            " to contact us about other issues.",
            br,
            "You can also ",
            a(href := routes.Page.loneBookmark("appeal"))("find here more information about appeals.")
          )
        )
      )
    )

  def apply(me: User)(implicit ctx: Context) =
    bits.layout("Appeal a moderation decision") {
      main(cls := "page page-small box box-pad appeal")(
        h1("Appeal"),
        div(cls := "nav-tree")(
          renderNode(
            {
              if (me.marks.clean) cleanMenu
              else ???
            },
            none
          )
        ),
        p(cls := "appeal__moderators text", dataIcon := "")(doNotMessageModerators())
      )
    }

  private def newAppeal(implicit ctx: Context) =
    discussion.renderForm(
      lila.appeal.Appeal.form,
      action = routes.Appeal.post.url,
      isNew = true,
      presets = none
    )

  private def renderHelp =
    div(cls := "appeal__help")(
      p(
        "If your account has been restricted for violation of ",
        a(href := routes.Page.tos)("the Lichess rules"),
        " you may file an appeal here."
      ),
      p(
        "You can read more about the appeal process ",
        a(href := routes.Page.loneBookmark("appeal"))("here.")
      )
    )
}

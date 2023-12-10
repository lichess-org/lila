package views.html.report

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

import controllers.routes

object thanks:

  def apply(userId: UserId, blocked: Boolean)(using PageContext) =

    val title = "Thanks for the report"

    val moreJs = embedJsUnsafeLoadThen("""
$('button.report-block').one('click', function() {
const $button = $(this);
$button.find('span').text('Blocking...');
fetch(this.dataset.action, {method:'post'})
  .then(() => $button.find('span').text('Blocked!'));
});
""")

    views.html.base.layout(title = title, moreJs = moreJs) {
      main(cls := "page-small box box-pad")(
        h1(cls := "box__top")(title),
        p("The moderators will review it very soon, and take appropriate action."),
        br,
        br,
        !blocked option p(
          "In the meantime, you can block this user: ",
          submitButton(
            attr("data-action") := routes.Relation.block(userId),
            cls                 := "report-block button",
            st.title            := trans.block.txt()
          )(span(cls := "text", dataIcon := licon.NotAllowed)("Block ", titleNameOrId(userId)))
        ),
        br,
        br,
        p(
          a(href := routes.Lobby.home)("Return to Lichess homepage")
        )
      )

    }

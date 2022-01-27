package views.html.report

import scala.annotation.nowarn

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object thanks {

  def apply(userId: String, blocked: Boolean)(implicit ctx: Context) = {

    val title = "Thanks for the report"

    @nowarn("msg=possible missing interpolator")
    val moreJs = embedJsUnsafeLoadThen("""
$('button.report-block').one('click', function() {
const $button = $(this);
$button.find('span').text('Blocking...');
fetch($button.data('action'), {method:'post'})
  .then(() => $button.find('span').text('Blocked!'));
});
""")

    views.html.base.layout(title = title, moreJs = moreJs) {
      main(cls := "page-small box box-pad")(
        h1(title),
        p("The moderators will review it very soon, and take appropriate action."),
        br,
        br,
        !blocked option p(
          "In the meantime, you can block this user: ",
          submitButton(
            attr("data-action") := routes.Relation.block(userId),
            cls := "report-block button",
            st.title := trans.block.txt()
          )(
            span(cls := "text", dataIcon := "")("Block ", titleNameOrId(userId))
          )
        ),
        br,
        br,
        p(
          a(href := routes.Lobby.home)("Return to Lichess homepage")
        )
      )

    }
  }
}

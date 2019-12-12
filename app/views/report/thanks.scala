package views.html.report

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User

import controllers.routes

object thanks {

  def apply(userId: String, blocked: Boolean)(implicit ctx: Context) = {

    val title = "Thanks for the report"

    val moreJs = embedJsUnsafe("""
$('button.report-block').one('click', function() {
var $button = $(this);
$button.find('span').text('Blocking...');
$.ajax({
url:$button.data('action'),
method:'post',
success: function() {
$button.find('span').text('Blocked!');
}
});
});
""")

    views.html.base.layout(title = title, moreJs = moreJs) {
      main(cls := "page-small box box-pad")(
        h1(title),
        p("The moderators will review it very soon, and take appropriate action."),
        br, br,
        !blocked option p(
          "In the meantime, you can block this user: ",
          submitButton(
            attr("data-action") := routes.Relation.block(userId),
            cls := "report-block button",
            st.title := trans.block.txt()
          )(
              span(cls := "text", dataIcon := "k")("Block ", usernameOrId(userId))
            )
        ),
        br, br,
        p(
          a(href := routes.Lobby.home)("Return to lichess homepage")
        )
      )

    }
  }
}

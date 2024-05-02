package views.report

import lila.app.UiEnv.{ *, given }

def thanks(userId: UserId, blocked: Boolean)(using ctx: Context) =
  val title = "Thanks for the report"
  Page(title)
    .js(
      embedJsUnsafeLoadThen("""
        $('button.report-block').one('click', function() {
        const $button = $(this);
        $button.find('span').text('Blocking...');
        fetch(this.dataset.action, {method:'post'})
          .then(() => $button.find('span').text('Blocked!'));
        });
        """)
    ):
      main(cls := "page-small box box-pad")(
        h1(cls := "box__top")(title),
        p("The moderators will review it very soon, and take appropriate action."),
        br,
        br,
        (!blocked).option(
          p(
            "In the meantime, you can block this user: ",
            submitButton(
              attr("data-action") := routes.Relation.block(userId),
              cls                 := "report-block button",
              st.title            := trans.site.block.txt()
            )(span(cls := "text", dataIcon := Icon.NotAllowed)("Block ", titleNameOrId(userId)))
          )
        ),
        br,
        br,
        p(
          a(href := routes.Lobby.home)("Return to Lichess homepage")
        )
      )

package lila.report
package ui

import lila.ui.*
import ScalatagsTemplate.{ *, given }
import lila.core.i18n.{ Translate, I18nKey as trans }

object ReportUi:

  def translatedReasonChoices(using Translate) =
    List(
      (Reason.Cheat.key, trans.site.cheat.txt()),
      (Reason.Comm.key, trans.site.insult.txt()),
      (Reason.Boost.key, trans.site.ratingManipulation.txt()),
      (Reason.Comm.key, trans.site.troll.txt()),
      (Reason.Sexism.key, "Sexual harassment or Sexist remarks"),
      (Reason.Username.key, trans.site.username.txt()),
      (Reason.Other.key, trans.site.other.txt())
    )

  def reportScore(score: Report.Score): Frag =
    span(cls := s"score ${score.color}")(score.value.toInt)

final class ReportUi(helpers: Helpers):
  import helpers.{ given, * }

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

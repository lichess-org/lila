package views.html.team

import controllers.routes
import play.api.data.Field
import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object admin {

  import trans.team._

  def leaders(t: lila.team.Team, form: Form[_])(implicit ctx: Context) = {
    val title = s"${t.name} • ${trans.team.teamLeaders.txt()}"
    views.html.base.layout(
      title = title,
      moreCss = frag(cssTag("team"), cssTag("tagify")),
      moreJs = jsModule("team.admin")
    ) {
      main(cls := "page-menu page-small")(
        bits.menu(none),
        div(cls := "page-menu__content box box-pad")(
          h1(title),
          p(
            "Only invite leaders that you fully trust. Team leaders can kick members and other leaders out of the team."
          ),
          postForm(cls := "leaders", action := routes.Team.leaders(t.id))(
            form3.group(form("leaders"), frag(usersWhoCanManageThisTeam()))(teamMembersAutoComplete(t)),
            form3.actions(
              a(href := routes.Team.show(t.id))(trans.cancel()),
              form3.submit(trans.save())
            )
          )
        )
      )
    }
  }

  def kick(t: lila.team.Team, form: Form[_])(implicit ctx: Context) = {

    val title = s"${t.name} • ${kickSomeone.txt()}"

    views.html.base.layout(
      title = title,
      moreCss = frag(cssTag("team"), cssTag("tagify")),
      moreJs = jsModule("team.admin")
    ) {
      main(cls := "page-menu page-small")(
        bits.menu(none),
        div(cls := "page-menu__content box box-pad")(
          h1(title),
          postForm(action := routes.Team.kick(t.id))(
            form3.group(form("members"), frag(whoToKick()))(teamMembersAutoComplete(t)),
            form3.actions(
              a(href := routes.Team.show(t.id))(trans.cancel()),
              form3.submit(trans.save())
            )
          )
        )
      )
    }
  }

  private def teamMembersAutoComplete(team: lila.team.Team)(field: Field) =
    form3.textarea(field)(rows := 2, dataRel := team.id)

  def pmAll(t: lila.team.Team, form: Form[_], tours: List[lila.tournament.Tournament])(implicit
      ctx: Context
  ) = {

    val title = s"${t.name} • ${messageAllMembers.txt()}"

    views.html.base.layout(
      title = title,
      moreCss = cssTag("team"),
      moreJs = embedJsUnsafeLoadThen("""
$('.copy-url-button').on('click', function(e) {
$('#form3-message').val($('#form3-message').val() + $(e.target).data('copyurl') + '\n')
})""")
    ) {
      main(cls := "page-menu page-small")(
        bits.menu(none),
        div(cls := "page-menu__content box box-pad")(
          h1(title),
          p(messageAllMembersLongDescription()),
          tours.nonEmpty option div(cls := "tournaments")(
            p(youWayWantToLinkOneOfTheseTournaments()),
            p(
              ul(
                tours.map { t =>
                  li(
                    tournamentLink(t),
                    " ",
                    momentFromNow(t.startsAt),
                    " ",
                    a(
                      dataIcon := "",
                      cls := "text copy-url-button",
                      data.copyurl := s"${netConfig.domain}${routes.Tournament.show(t.id).url}"
                    )
                  )
                }
              )
            ),
            br
          ),
          postForm(cls := "form3", action := routes.Team.pmAllSubmit(t.id))(
            form3.group(
              form("message"),
              trans.message(),
              help = raw("You can send up to 10 team messages per week.").some
            )(form3.textarea(_)(rows := 10)),
            form3.actions(
              a(href := routes.Team.show(t.slug))(trans.cancel()),
              form3.submit(trans.send())
            )
          )
        )
      )
    }
  }
}

package views.html.team

import controllers.routes
import play.api.data.Field
import play.api.data.Form
import play.api.i18n.Lang

import lila.api.Context
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

object admin:

  import trans.team.*

  def leaders(t: lila.team.Team, form: Form[?])(implicit ctx: Context) =
    views.html.base.layout(
      title = s"${t.name} • ${teamLeaders.txt()}",
      moreCss = frag(cssTag("team"), cssTag("tagify")),
      moreJs = jsModule("team.admin")
    ) {
      main(cls := "page-menu page-small")(
        bits.menu(none),
        div(cls := "page-menu__content box box-pad")(
          adminTop(t, teamLeaders),
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

  def kick(t: lila.team.Team, form: Form[?])(implicit ctx: Context) =
    views.html.base.layout(
      title = s"${t.name} • ${kickSomeone.txt()}",
      moreCss = frag(cssTag("team"), cssTag("tagify")),
      moreJs = jsModule("team.admin")
    ) {
      main(cls := "page-menu page-small")(
        bits.menu(none),
        div(cls := "page-menu__content box box-pad")(
          adminTop(t, kickSomeone),
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

  private def teamMembersAutoComplete(team: lila.team.Team)(field: Field) =
    form3.textarea(field)(rows := 2, dataRel := team.id)

  def pmAll(
      t: lila.team.Team,
      form: Form[?],
      tours: List[lila.tournament.Tournament],
      unsubs: Int,
      limiter: (Int, Instant)
  )(using ctx: Context) =
    views.html.base.layout(
      title = s"${t.name} • ${messageAllMembers.txt()}",
      moreCss = cssTag("team"),
      moreJs = embedJsUnsafeLoadThen("""
$('.copy-url-button').on('click', function(e) {
$('#form3-message').val($('#form3-message').val() + $(e.target).data('copyurl') + '\n')
})""")
    ) {
      main(cls := "page-menu page-small")(
        bits.menu(none),
        div(cls := "page-menu__content box box-pad")(
          adminTop(t, messageAllMembers),
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
                      dataIcon     := "",
                      cls          := "text copy-url-button",
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
              help = frag(
                pluralizeLocalize("member", unsubs),
                " out of ",
                t.nbMembers.localize,
                " (",
                f"${(unsubs * 100d) / t.nbMembers}%1.1f",
                "%)",
                " have unsubscribed from messages."
              ).some
            )(form3.textarea(_)(rows := 10)),
            limiter match
              case (remaining, until) =>
                frag(
                  p(cls := (remaining <= 0).option("error"))(
                    "You can send up to ",
                    lila.app.mashup.TeamInfo.pmAllCredits,
                    " team messages per week. ",
                    strong(remaining),
                    " messages remaining until ",
                    momentFromNowOnce(until),
                    "."
                  ),
                  form3.actions(
                    a(href := routes.Team.show(t.slug))(trans.cancel()),
                    remaining > 0 option form3.submit(trans.send())
                  )
                )
          )
        )
      )
    }

  private def adminTop(t: lila.team.Team, i18n: lila.i18n.I18nKey)(using Lang) =
    boxTop(
      h1(a(href := routes.Team.show(t.slug))(t.name), " • ", i18n())
    )

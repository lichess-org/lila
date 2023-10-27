package views.html.team

import controllers.routes
import play.api.data.{ Field, Form }
import play.api.i18n.Lang

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.team.{ Team, TeamSecurity }

object admin:

  import trans.team.*

  def leaders(
      t: Team.WithLeaders,
      addLeaderForm: Form[UserStr],
      permsForm: Form[Seq[TeamSecurity.LeaderData]]
  )(using PageContext) =
    views.html.base.layout(
      title = s"${t.name} • ${teamLeaders.txt()}",
      moreCss = frag(cssTag("team"), cssTag("tagify")),
      moreJs = jsModule("team.admin")
    ):
      val dataLabel = attrData("label")
      main(cls := "page-menu")(
        bits.menu(none),
        div(cls := "page-menu__content box")(
          adminTop(t.team, teamLeaders()),
          standardFlash.map(div(cls := "box__pad")(_)),
          postForm(
            cls    := "team-add-leader box__pad complete-parent",
            action := routes.Team.addLeader(t.id)
          )(
            errMsg(addLeaderForm),
            div(cls := "team-add-leader__input")(
              st.input(name := "name", attrData("team-id") := t.id, placeholder := "Add a new leader"),
              form3.submit("Add")
            )
          ),
          postForm(cls := "team-permissions form3", action := routes.Team.permissions(t.id))(
            globalError(permsForm).map(_(cls := "box__pad text", dataIcon := licon.CautionTriangle)),
            table(cls := "slist slist-pad slist-resp")(
              thead:
                tr(
                  th,
                  t.leaders.mapWithIndex: (l, i) =>
                    th(
                      userIdLink(l.user.some, withOnline = false),
                      form3.hidden(s"leaders[$i].name", l.user)
                    ),
                )
              ,
              tbody:
                TeamSecurity.Permission.values.toList.mapWithIndex: (perm, pi) =>
                  tr(
                    th(
                      strong(perm.name),
                      p(perm.desc)
                    ),
                    t.leaders.mapWithIndex: (l, li) =>
                      td(dataLabel := l.user):
                        form3.cmnToggle(
                          fieldId = s"leaders-$li-perms-${perm.key}",
                          fieldName = s"leaders[$li].perms[]",
                          checked = (0 to TeamSecurity.Permission.values.size).exists: i =>
                            permsForm.data.get(s"leaders[$li].perms[$i]").contains(perm.key),
                          value = perm.key
                        )
                  )
            ),
            p(cls := "form-help box__pad")("To remove a leader, remove all permissions."),
            form3.actions(cls := "box__pad")(
              a(href := routes.Team.show(t.id))(trans.cancel()),
              form3.submit(trans.save())
            )
          )
        )
      )

  def kick(t: Team, form: Form[String], blocklistForm: Form[String])(using PageContext) =
    views.html.base.layout(
      title = s"${t.name} • ${kickSomeone.txt()}",
      moreCss = frag(cssTag("team"), cssTag("tagify")),
      moreJs = jsModule("team.admin")
    ):
      main(cls := "page-menu page-small")(
        bits.menu(none),
        div(cls := "page-menu__content")(
          div(cls := "box box-pad")(
            adminTop(t, kickSomeone()),
            postForm(action := routes.Team.kick(t.id))(
              form3.group(form("members"), frag(whoToKick()))(teamMembersAutoComplete(t)),
              form3.actions(
                a(href := routes.Team.show(t.id))(trans.cancel()),
                form3.submit(lila.i18n.I18nKeys.study.kick())
              )
            )
          ),
          br,
          div(cls := "box box-pad")(
            adminTop(t, "User blocklist"),
            postForm(action := routes.Team.blocklist(t.id))(
              form3
                .group(
                  blocklistForm("names"),
                  frag("List of usernames who cannot join the team. One per line.")
                )(
                  form3.textarea(_)(rows := 4)
                ),
              form3.actions(
                a(href := routes.Team.show(t.id))(trans.cancel()),
                form3.submit(trans.save())
              )
            )
          )
        )
      )

  private def teamMembersAutoComplete(team: Team)(field: Field) =
    form3.textarea(field)(rows := 2, dataRel := team.id)

  def pmAll(
      t: Team,
      form: Form[?],
      tours: List[lila.tournament.Tournament],
      unsubs: Int,
      limiter: (Int, Instant)
  )(using ctx: PageContext) =
    views.html.base.layout(
      title = s"${t.name} • ${messageAllMembers.txt()}",
      moreCss = cssTag("team"),
      moreJs = embedJsUnsafeLoadThen("""
$('.copy-url-button').on('click', function(e) {
$('#form3-message').val($('#form3-message').val() + e.target.dataset.copyurl + '\n')
})""")
    ):
      main(cls := "page-menu page-small")(
        bits.menu(none),
        div(cls := "page-menu__content box box-pad")(
          adminTop(t, messageAllMembers()),
          p(messageAllMembersLongDescription()),
          tours.nonEmpty option div(cls := "tournaments")(
            p(youWayWantToLinkOneOfTheseTournaments()),
            p:
              ul:
                tours.map: t =>
                  li(
                    tournamentLink(t),
                    " ",
                    momentFromNow(t.startsAt),
                    " ",
                    a(
                      dataIcon     := licon.Forward,
                      cls          := "text copy-url-button",
                      data.copyurl := s"${netConfig.domain}${routes.Tournament.show(t.id).url}"
                    )
                  )
            ,
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

  private def adminTop(t: Team, title: Frag)(using Lang) =
    boxTop:
      h1(a(href := routes.Team.show(t.slug))(t.name), " • ", title)

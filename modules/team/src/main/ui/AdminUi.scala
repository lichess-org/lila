package lila.team
package ui

import play.api.data.{ Field, Form }

import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class AdminUi(helpers: Helpers, bits: TeamUi):
  import helpers.{ *, given }
  import trans.team as trt
  import bits.{ TeamPage, menu }

  def leaders(
      t: Team.WithLeaders,
      addLeaderForm: Form[UserStr],
      permsForm: Form[List[TeamSecurity.LeaderData]]
  )(using Context) =
    TeamPage(s"${t.name} • ${trans.team.teamLeaders.txt()}")
      .js(Esm("mod.teamAdmin"))
      .css("bits.tagify"):
        val dataLabel = attrData("label")
        main(cls := "page-menu")(
          menu(none),
          div(cls := "page-menu__content box")(
            adminTop(t.team, trt.teamLeaders()),
            standardFlash.map(div(cls := "box__pad")(_)),
            postForm(
              cls := "team-add-leader box__pad complete-parent",
              action := routes.Team.addLeader(t.id)
            )(
              errMsg(addLeaderForm),
              div(cls := "team-add-leader__input")(
                st.input(name := "name", attrData("team-id") := t.id, placeholder := "Add a new leader"),
                form3.submit("Add")
              )
            ),
            postForm(cls := "team-permissions form3", action := routes.Team.permissions(t.id))(
              globalError(permsForm).map(_(cls := "box__pad text", dataIcon := Icon.CautionTriangle)),
              table(cls := "slist slist-pad slist-resp")(
                thead:
                  tr(
                    th,
                    t.leaders.mapWithIndex: (l, i) =>
                      th(
                        userIdLink(l.user.some, withOnline = false),
                        form3.hidden(s"leaders[$i].name", l.user)
                      )
                  )
                ,
                tbody:
                  TeamSecurity.Permission.values.toList.map: perm =>
                    tr(
                      th(
                        strong(perm.name),
                        p(perm.desc)
                      ),
                      t.leaders.mapWithIndex: (l, li) =>
                        td(dataLabel := l.user):
                          form3.nativeCheckbox(
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
                a(href := routes.Team.show(t.id))(trans.site.cancel()),
                form3.submit(trans.site.save())
              )
            )
          )
        )

  def kick(t: Team, form: Form[String], blocklistForm: Form[String])(using Context) =
    TeamPage(s"${t.name} • ${trans.team.kickSomeone.txt()}")
      .js(Esm("mod.teamAdmin"))
      .css("bits.tagify"):
        main(cls := "page-menu page-small")(
          menu(none),
          div(cls := "page-menu__content")(
            div(cls := "box box-pad")(
              adminTop(t, trt.kickSomeone()),
              postForm(action := routes.Team.kick(t.id))(
                form3.group(form("members"), frag(trt.whoToKick()))(teamMembersAutoComplete(t)),
                form3.actions(
                  a(href := routes.Team.show(t.id))(trans.site.cancel()),
                  form3.submit(lila.core.i18n.I18nKey.study.kick())
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
                  a(href := routes.Team.show(t.id))(trans.site.cancel()),
                  form3.submit(trans.site.save())
                )
              )
            )
          )
        )

  def pmAll(
      t: Team,
      form: Form[?],
      tours: Option[Frag],
      unsubs: Int,
      limiter: (Int, Instant),
      credits: Int
  )(using ctx: Context) =
    TeamPage(s"${t.name} • ${trans.team.messageAllMembers.txt()}").js(esmInitBit("pmAll")):
      main(cls := "page-menu page-small")(
        menu(none),
        div(cls := "page-menu__content box box-pad")(
          adminTop(t, trt.messageAllMembers()),
          p(trt.messageAllMembersLongDescription()),
          tours,
          postForm(cls := "form3", action := routes.Team.pmAllSubmit(t.id))(
            form3.group(
              form("message"),
              trans.site.message(),
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
                    credits,
                    " team messages per week. ",
                    strong(remaining),
                    " messages remaining until ",
                    momentFromNowOnce(until),
                    "."
                  ),
                  form3.actions(
                    a(href := routes.Team.show(t.slug))(trans.site.cancel()),
                    (remaining > 0).option(form3.submit(trans.site.send()))
                  )
                )
          )
        )
      )

  private def teamMembersAutoComplete(team: Team)(field: Field) =
    form3.textarea(field)(rows := 2, dataRel := team.id)

  private def adminTop(t: Team, title: Frag) =
    boxTop:
      h1(a(href := routes.Team.show(t.slug))(t.name), " • ", title)

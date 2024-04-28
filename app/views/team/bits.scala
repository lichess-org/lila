package views.team

import play.api.data.Form
import scalalib.paginator.Paginator

import lila.app.templating.Environment.{ *, given }
import lila.common.{ Markdown, MarkdownRender }
import lila.core.captcha.Captcha
import lila.team.Team

private lazy val bits = lila.team.ui.TeamUi(helpers)(using env.executor)

export bits.{ list, membersPage }

lazy val form = lila.team.ui.FormUi(helpers, bits)(views.base.captcha.apply)

object request:
  lazy val ui = lila.team.ui.RequestUi(helpers, bits)

  def requestForm(t: lila.team.Team, form: Form[?])(using PageContext) =
    bits.teamPage(ui.requestForm(t, form))

  def all(requests: List[lila.team.RequestWithUser])(using PageContext) =
    bits.teamPage(ui.all(requests))

  def declined(team: lila.team.Team, requests: Paginator[lila.team.RequestWithUser], search: Option[UserStr])(
      using PageContext
  ) =
    bits.teamPage(ui.declined(team, requests, search)(_(EsmInit("mod.teamAdmin"))))

object admin:
  private lazy val adminUi = lila.team.ui.AdminUi(helpers, bits)

  def leaders(
      t: Team.WithLeaders,
      addLeaderForm: Form[UserStr],
      permsForm: Form[Seq[lila.team.TeamSecurity.LeaderData]]
  )(using PageContext) =
    bits.teamPage:
      adminUi.leaders(t, addLeaderForm, permsForm)(_(EsmInit("mod.teamAdmin")).css(cssTag("tagify")))

  def kick(t: Team, form: Form[String], blocklistForm: Form[String])(using PageContext) =
    bits.teamPage:
      adminUi.kick(t, form, blocklistForm)(_(EsmInit("mod.teamAdmin")).css(cssTag("tagify")))

  def pmAll(
      t: Team,
      form: Form[?],
      tours: List[lila.tournament.Tournament],
      unsubs: Int,
      limiter: (Int, Instant)
  )(using PageContext) =
    val toursFrag = tours.nonEmpty.option:
      div(cls := "tournaments")(
        p(trans.team.youWayWantToLinkOneOfTheseTournaments()),
        p:
          ul:
            tours.map: t =>
              li(
                views.tournament.ui.tournamentLink(t),
                " ",
                momentFromNow(t.startsAt),
                " ",
                a(
                  dataIcon     := Icon.Forward,
                  cls          := "text copy-url-button",
                  data.copyurl := s"${netConfig.domain}${routes.Tournament.show(t.id).url}"
                )
              )
        ,
        br
      )
    bits.teamPage:
      adminUi.pmAll(t, form, toursFrag, unsubs, limiter, lila.app.mashup.TeamInfo.pmAllCredits):
        _.js(embedJsUnsafeLoadThen(adminUi.pmAllJs))

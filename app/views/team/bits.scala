package views.team

import play.api.data.Form
import scalalib.paginator.Paginator

import lila.app.templating.Environment.{ *, given }
import lila.common.{ Markdown, MarkdownRender }
import lila.core.captcha.Captcha
import lila.team.Team

private lazy val bits = lila.team.ui.TeamUi(helpers)(using env.executor)

private lazy val layoutConfig: Layout.Build = _.cssTag("team")

private def teamPage(p: lila.ui.Page)(using PageContext) =
  p.contramap(layoutConfig)(_(infiniteScrollTag))

def members(t: Team, pager: Paginator[lila.team.TeamMember.UserAndDate])(using PageContext) =
  teamPage(bits.membersPage(t, pager))

object list:
  def search(text: String, teams: Paginator[Team.WithMyLeadership])(using PageContext) =
    teamPage(bits.list.search(text, teams))

  def all(teams: Paginator[Team.WithMyLeadership])(using PageContext) =
    teamPage(bits.list.all(teams))

  def mine(teams: List[Team.WithMyLeadership])(using ctx: PageContext) =
    teamPage(bits.list.mine(teams))

  def ledByMe(teams: List[Team])(using PageContext) =
    teamPage(bits.list.ledByMe(teams))

object form:
  private lazy val formUi = lila.team.ui.FormUi(helpers, bits)(views.base.captcha.apply)
  def create(form: Form[?], captcha: Captcha)(using PageContext) =
    teamPage:
      formUi.create(form, captcha):
        _.copy(modules = captchaTag)
  def edit(t: Team, form: Form[?], member: Option[lila.team.TeamMember])(using PageContext) =
    teamPage:
      formUi.edit(t, form, member)(_(jsModule("bits.team")))

object request:
  lazy val ui = lila.team.ui.RequestUi(helpers, bits)

  def requestForm(t: lila.team.Team, form: Form[?])(using PageContext) =
    teamPage(ui.requestForm(t, form))

  def all(requests: List[lila.team.RequestWithUser])(using PageContext) =
    teamPage(ui.all(requests))

  def declined(team: lila.team.Team, requests: Paginator[lila.team.RequestWithUser], search: Option[UserStr])(
      using PageContext
  ) =
    teamPage(ui.declined(team, requests, search)(_(jsModule("mod.teamAdmin"))))

object admin:
  private lazy val adminUi = lila.team.ui.AdminUi(helpers, bits)

  def leaders(
      t: Team.WithLeaders,
      addLeaderForm: Form[UserStr],
      permsForm: Form[Seq[lila.team.TeamSecurity.LeaderData]]
  )(using PageContext) =
    teamPage:
      adminUi.leaders(t, addLeaderForm, permsForm)(_(jsModule("mod.teamAdmin")).css(cssTag("tagify")))

  def kick(t: Team, form: Form[String], blocklistForm: Form[String])(using PageContext) =
    teamPage:
      adminUi.kick(t, form, blocklistForm)(_(jsModule("mod.teamAdmin")).css(cssTag("tagify")))

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
    teamPage:
      adminUi.pmAll(t, form, toursFrag, unsubs, limiter, lila.app.mashup.TeamInfo.pmAllCredits):
        _.js(embedJsUnsafeLoadThen(adminUi.pmAllJs))

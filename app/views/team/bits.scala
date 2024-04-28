package views.team

import play.api.data.Form
import scalalib.paginator.Paginator

import lila.app.templating.Environment.{ *, given }
import lila.common.{ Markdown, MarkdownRender }
import lila.core.captcha.Captcha
import lila.team.Team

private lazy val bits = lila.team.ui.TeamUi(helpers)(using env.executor)

private lazy val layoutConfig = layoutDefault.copy(moreCss = cssTag("team"))

private def layout(title: String, config: Layout.Build = Layout.default)(body: Frag)(using PageContext) =
  views.base.layout.from(_ => config(layoutConfig).title(title)(infiniteScrollTag))(body)

def members(t: Team, pager: Paginator[lila.team.TeamMember.UserAndDate])(using PageContext) =
  val (og, body) = bits.membersPage(t, pager)
  layout(t.name, _(og))(body)

object form:
  private lazy val formUi = lila.team.ui.FormUi(helpers, bits)(views.base.captcha.apply)
  def create(form: Form[?], captcha: Captcha)(using PageContext) =
    views.base.layout(
      title = trans.team.newTeam.txt(),
      moreCss = cssTag("team"),
      modules = captchaTag
    )(formUi.create(form, captcha))
  def edit(t: Team, form: Form[?], member: Option[lila.team.TeamMember])(using ctx: PageContext) =
    layout(s"Edit Team ${t.name}", _(jsModule("bits.team"))):
      formUi.edit(t, form, member)

object request:
  lazy val ui = lila.team.ui.RequestUi(helpers, bits)

  def requestForm(t: lila.team.Team, form: Form[?])(using PageContext) =
    views.base.layout(
      title = s"${trans.team.joinTeam.txt()} ${t.name}",
      moreCss = cssTag("team")
    )(ui.requestForm(t, form))

  def all(requests: List[lila.team.RequestWithUser])(using PageContext) =
    val title = trans.team.xJoinRequests.pluralSameTxt(requests.size)
    layout(title)(ui.all(requests, title))

  def declined(
      team: lila.team.Team,
      requests: Paginator[lila.team.RequestWithUser],
      search: Option[UserStr]
  )(using PageContext) =
    val title = s"${team.name} • ${trans.team.declinedRequests.txt()}"
    views.base.layout(
      title = title,
      moreCss = frag(cssTag("team")),
      modules = jsModule("mod.teamAdmin")
    )(ui.declined(team, requests, search, title))

object admin:
  private lazy val adminUi = lila.team.ui.AdminUi(helpers, bits)

  def leaders(
      t: Team.WithLeaders,
      addLeaderForm: Form[UserStr],
      permsForm: Form[Seq[lila.team.TeamSecurity.LeaderData]]
  )(using PageContext) =
    views.base.layout(
      title = s"${t.name} • ${trans.team.teamLeaders.txt()}",
      moreCss = frag(cssTag("team"), cssTag("tagify")),
      modules = jsModule("mod.teamAdmin")
    )(adminUi.leaders(t, addLeaderForm, permsForm))

  def kick(t: Team, form: Form[String], blocklistForm: Form[String])(using PageContext) =
    views.base.layout(
      title = s"${t.name} • ${trans.team.kickSomeone.txt()}",
      moreCss = frag(cssTag("team"), cssTag("tagify")),
      modules = jsModule("mod.teamAdmin")
    )(adminUi.kick(t, form, blocklistForm))

  def pmAll(
      t: Team,
      form: Form[?],
      tours: List[lila.tournament.Tournament],
      unsubs: Int,
      limiter: (Int, Instant)
  )(using ctx: PageContext) =
    views.base.layout(
      title = s"${t.name} • ${trans.team.messageAllMembers.txt()}",
      moreCss = cssTag("team"),
      moreJs = embedJsUnsafeLoadThen(adminUi.pmAllJs)
    ):
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
      adminUi.pmAll(t, form, toursFrag, unsubs, limiter, lila.app.mashup.TeamInfo.pmAllCredits)

package views.html.team

import controllers.routes
import controllers.team.routes.{ Team as teamRoutes }
import scala.util.chaining.*

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.{ Markdown, MarkdownRender }
import lila.team.Team

object bits:

  import trans.team.*

  def menu(currentTab: Option[String])(using ctx: PageContext) =
    val tab = ~currentTab
    st.nav(cls := "page-menu__menu subnav")(
      (ctx.teamNbRequests > 0) option
        a(cls := tab.active("requests"), href := teamRoutes.requests)(
          xJoinRequests.pluralSame(ctx.teamNbRequests)
        ),
      ctx.isAuth option
        a(cls := tab.active("mine"), href := teamRoutes.mine)(
          myTeams()
        ),
      ctx.isAuth option
        a(cls := tab.active("leader"), href := teamRoutes.leader)(
          leaderTeams()
        ),
      a(cls := tab.active("all"), href := teamRoutes.all())(
        allTeams()
      ),
      ctx.isAuth option
        a(cls := tab.active("form"), href := teamRoutes.form)(
          newTeam()
        )
    )

  private[team] object markdown:
    private val renderer = MarkdownRender(header = true, list = true, table = true)
    private val cache = lila.memo.CacheApi.scaffeineNoScheduler
      .expireAfterAccess(10 minutes)
      .maximumSize(1024)
      .build[Markdown, Html]()
    def apply(team: Team, text: Markdown): Frag = rawHtml(cache.get(text, renderer(s"team:${team.id}")))

  private[team] def teamTr(t: Team.WithMyLeadership)(using ctx: Context) =
    val isMine = isMyTeamSync(t.id)
    tr(cls := "paginated")(
      td(cls := "subject")(
        a(
          dataIcon := licon.Group,
          cls := List(
            "team-name text" -> true,
            "mine"           -> isMine
          ),
          href := teamRoutes.show(t.id)
        )(
          t.name,
          t.flair map teamFlair,
          t.amLeader option em("leader")
        ),
        ~t.intro: String
      ),
      td(cls := "info")(
        p(nbMembers.plural(t.nbMembers, t.nbMembers.localize)),
        isMine option form(action := teamRoutes.quit(t.id), method := "post")(
          submitButton(cls := "button button-empty button-red button-thin confirm team__quit")(quitTeam.txt())
        )
      )
    )

  private[team] def layout(
      title: String,
      openGraph: Option[lila.app.ui.OpenGraph] = None,
      moreJs: Frag = emptyFrag
  )(body: Frag)(using PageContext) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("team"),
      moreJs = frag(infiniteScrollTag, moreJs),
      openGraph = openGraph
    )(body)

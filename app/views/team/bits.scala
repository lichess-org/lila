package views.html.team

import scala.util.chaining._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object bits {

  import trans.team._

  def link(teamId: lila.team.Team.ID): Frag =
    a(href := routes.Team.show(teamId))(teamIdToName(teamId))

  def link(team: lila.team.Team): Frag =
    a(href := routes.Team.show(team.id))(team.name)

  def menu(currentTab: Option[String])(implicit ctx: Context) =
    ~currentTab pipe { tab =>
      st.nav(cls := "page-menu__menu subnav")(
        (ctx.teamNbRequests > 0) option
          a(cls := tab.active("requests"), href := routes.Team.requests)(
            xJoinRequests.pluralSame(ctx.teamNbRequests)
          ),
        ctx.isAuth option
          a(cls := tab.active("mine"), href := routes.Team.mine)(
            myTeams()
          ),
        ctx.isAuth option
          a(cls := tab.active("leader"), href := routes.Team.leader)(
            "Leader teams"
          ),
        a(cls := tab.active("all"), href := routes.Team.all())(
          allTeams()
        ),
        ctx.isAuth option
          a(cls := tab.active("form"), href := routes.Team.form)(
            newTeam()
          )
      )
    }

  private[team] def teamTr(t: lila.team.Team)(implicit ctx: Context) = {
    val isMine = myTeam(t.id)
    tr(cls := "paginated")(
      td(cls := "subject")(
        a(
          dataIcon := "",
          cls := List(
            "team-name text" -> true,
            "mine"           -> isMine
          ),
          href := routes.Team.show(t.id)
        )(
          t.name,
          ctx.userId.exists(t.leaders.contains) option em("leader")
        ),
        shorten(t.description, 200)
      ),
      td(cls := "info")(
        p(nbMembers.plural(t.nbMembers, t.nbMembers.localize)),
        isMine option form(action := routes.Team.quit(t.id), method := "post")(
          submitButton(cls := "button button-empty button-red button-thin confirm team__quit")(quitTeam.txt())
        )
      )
    )
  }

  private[team] def layout(
      title: String,
      openGraph: Option[lila.app.ui.OpenGraph] = None,
      moreJs: Frag = emptyFrag
  )(body: Frag)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("team"),
      moreJs = frag(infiniteScrollTag, moreJs),
      openGraph = openGraph
    )(body)
}

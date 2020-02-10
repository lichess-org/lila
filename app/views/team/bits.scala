package views.html.team

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object bits {

  import trans.team._

  def menu(currentTab: Option[String])(implicit ctx: Context) = ~currentTab |> { tab =>
    st.nav(cls := "page-menu__menu subnav")(
      (ctx.teamNbRequests > 0) option
        a(cls := tab.active("requests"), href := routes.Team.requests())(
          xJoinRequests(ctx.teamNbRequests)
        ),
      ctx.isAuth option
        a(cls := tab.active("mine"), href := routes.Team.mine())(
          myTeams()
        ),
      a(cls := tab.active("all"), href := routes.Team.all())(
        allTeams()
      ),
      ctx.isAuth option
        a(cls := tab.active("form"), href := routes.Team.form())(
          newTeam()
        )
    )
  }

  private[team] def teamTr(t: lila.team.Team)(implicit ctx: Context) = tr(cls := "paginated")(
    td(cls := "subject")(
      a(
        dataIcon := "f",
        cls := List(
          "team-name text" -> true,
          "mine"           -> myTeam(t.id)
        ),
        href := routes.Team.show(t.id)
      )(t.name),
      shorten(t.description, 200)
    ),
    td(cls := "info")(
      p(nbMembers.plural(t.nbMembers, t.nbMembers.localize))
    )
  )

  private[team] def layout(title: String, openGraph: Option[lila.app.ui.OpenGraph] = None)(
      body: Frag
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("team"),
      moreJs = infiniteScrollTag,
      openGraph = openGraph
    )(body)
}

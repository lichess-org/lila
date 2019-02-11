package views.html
package team

import play.twirl.api.Html

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator

import controllers.routes

object bits {

  def menu(currentTab: Option[String])(implicit ctx: Context) = ~currentTab |> { tab =>
    st.nav(cls := "page-menu__menu subnav")(
      (ctx.teamNbRequests > 0) option
        a(cls := tab.active("requests"), href := routes.Team.requests())(
          ctx.teamNbRequests, " join requests"
        ),
      ctx.me.??(_.canTeam) option
        a(cls := tab.active("mine"), href := routes.Team.mine())(
          trans.myTeams.frag()
        ),
      a(cls := tab.active("all"), href := routes.Team.all())(
        trans.allTeams.frag()
      ),
      ctx.me.??(_.canTeam) option
        a(cls := tab.active("form"), href := routes.Team.form())(
          trans.newTeam.frag()
        )
    )
  }

  def teamTr(t: lila.team.Team)(implicit ctx: Context) =
    tr(cls := "paginated")(
      td(cls := "subject")(
        a(cls := "team-name", href := routes.Team.show(t.id))(
          iconTag("f")(cls := List(
            "is-green" -> myTeam(t.id),
            "text" -> true
          )),
          t.name
        ),
        shorten(t.description, 200)
      ),
      td(cls := "info")(
        p(trans.nbMembers.pluralFrag(t.nbMembers, t.nbMembers.localize))
      )
    )

  def all(teams: Paginator[lila.team.Team])(implicit ctx: Context) = team.list(
    name = trans.teams.txt(),
    teams = teams,
    next = teams.nextPage map { n => routes.Team.all(n) },
    tab = "all"
  )

  def layout(title: String, openGraph: Option[lila.app.ui.OpenGraph] = None)(body: Html)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = responsiveCssTag("team"),
      moreJs = infiniteScrollTag,
      openGraph = openGraph,
      responsive = true
    )(body)
}

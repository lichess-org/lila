package views.html.team

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator

import controllers.routes

object list {

  import trans.team._

  def search(text: String, teams: Paginator[lila.team.Team])(implicit ctx: Context) =
    list(
      name = s"""${trans.search.search.txt()} "$text"""",
      teams = teams,
      nextPageUrl = n => routes.Team.search(text, n).url,
      search = text
    )

  def all(teams: Paginator[lila.team.Team])(implicit ctx: Context) =
    list(
      name = trans.team.teams.txt(),
      teams = teams,
      nextPageUrl = n => routes.Team.all(n).url
    )

  def mine(teams: List[lila.team.Team], tooMany: Boolean = false)(implicit ctx: Context) =
    bits.layout(title = myTeams.txt()) {
      main(cls := "team-list page-menu")(
        bits.menu("mine".some),
        div(cls := "page-menu__content box")(
          h1(myTeams()),
          standardFlash(),
          ctx.me.ifTrue(tooMany) map { me =>
            flashMessage(cls := "flash-failure")(
              s"You have joined ${teams.size} out of ${lila.team.Team.maxJoin(me)} teams. Leave some teams before you can join others."
            )
          },
          table(cls := "slist slist-pad")(
            if (teams.nonEmpty) tbody(teams.map(bits.teamTr(_)))
            else noTeam()
          )
        )
      )
    }

  def ledByMe(teams: List[lila.team.Team])(implicit ctx: Context) =
    bits.layout(title = myTeams.txt()) {
      main(cls := "team-list page-menu")(
        bits.menu("leader".some),
        div(cls := "page-menu__content box")(
          h1(teamsIlead()),
          standardFlash(),
          table(cls := "slist slist-pad")(
            if (teams.nonEmpty) tbody(teams.map(bits.teamTr(_)))
            else noTeam()
          )
        )
      )
    }

  private def noTeam()(implicit ctx: Context) =
    tbody(
      tr(
        td(colspan := "2")(
          br,
          noTeamFound()
        )
      )
    )

  private def list(
      name: String,
      teams: Paginator[lila.team.Team],
      nextPageUrl: Int => String,
      search: String = ""
  )(implicit ctx: Context) =
    bits.layout(title = "%s - page %d".format(name, teams.currentPage)) {
      main(cls := "team-list page-menu")(
        bits.menu("all".some),
        div(cls := "page-menu__content box")(
          div(cls := "box__top")(
            h1(name),
            div(cls := "box__top__actions")(
              st.form(cls := "search", action := routes.Team.search())(
                input(st.name := "text", value := search, placeholder := trans.search.search.txt())
              )
            )
          ),
          standardFlash(),
          table(cls := "slist slist-pad")(
            if (teams.nbResults > 0)
              tbody(cls := "infinite-scroll")(
                teams.currentPageResults map { bits.teamTr(_) },
                pagerNextTable(teams, nextPageUrl)
              )
            else noTeam()
          )
        )
      )
    }
}

package views.html.team

import lila.api.Context
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.paginator.Paginator

import controllers.routes

object list:

  import trans.team.*

  def search(text: String, teams: Paginator[lila.team.Team])(using Context) =
    list(
      name = s"""${trans.search.search.txt()} "$text"""",
      teams = teams,
      nextPageUrl = n => routes.Team.search(text, n).url,
      search = text
    )

  def all(teams: Paginator[lila.team.Team])(using Context) =
    list(
      name = trans.team.teams.txt(),
      teams = teams,
      nextPageUrl = n => routes.Team.all(n).url
    )

  def mine(teams: List[lila.team.Team])(using ctx: Context) =
    bits.layout(title = myTeams.txt()) {
      main(cls := "team-list page-menu")(
        bits.menu("mine".some),
        div(cls := "page-menu__content box")(
          h1(cls := "box__top")(myTeams()),
          standardFlash,
          ctx.me.filter(me => teams.size > lila.team.Team.maxJoin(me)) map { me =>
            flashMessage("failure")(
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

  def ledByMe(teams: List[lila.team.Team])(using Context) =
    bits.layout(title = myTeams.txt()) {
      main(cls := "team-list page-menu")(
        bits.menu("leader".some),
        div(cls := "page-menu__content box")(
          h1(cls := "box__top")(teamsIlead()),
          standardFlash,
          table(cls := "slist slist-pad")(
            if (teams.nonEmpty) tbody(teams.map(bits.teamTr(_)))
            else noTeam()
          )
        )
      )
    }

  private def noTeam()(using Context) =
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
  )(using Context) =
    bits.layout(title = "%s - page %d".format(name, teams.currentPage)) {
      main(cls := "team-list page-menu")(
        bits.menu("all".some),
        div(cls := "page-menu__content box")(
          boxTop(
            h1(name),
            div(cls := "box__top__actions")(
              st.form(cls := "search", action := routes.Team.search())(
                input(st.name := "text", value := search, placeholder := trans.search.search.txt())
              )
            )
          ),
          standardFlash,
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

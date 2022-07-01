package views.html.team

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.team.{ Team, TeamMember }
import lila.relation.Relation

object members {

  import trans.team._

  def apply(t: Team, pager: Paginator[TeamMember.UserAndDate])(implicit ctx: Context) =
    bits.layout(
      title = t.name,
      openGraph = lila.app.ui
        .OpenGraph(
          title = s"${t.name} • ${trans.team.teamRecentMembers.txt()}",
          url = s"$netBaseUrl${routes.Team.show(t.id).url}",
          description = shorten(t.description.value, 152)
        )
        .some
    ) {
      main(cls := "page-small box")(
        h1(
          views.html.team.bits.link(t),
          " • ",
          nbMembers.plural(t.nbMembers, t.nbMembers.localize)
        ),
        table(cls := "team-members slist slist-pad")(
          tbody(cls := "infinite-scroll")(
            pager.currentPageResults.map { case TeamMember.UserAndDate(u, date) =>
              tr(cls := "paginated")(
                td(lightUserLink(u)),
                td(momentFromNowOnce(date))
              )
            },
            pagerNextTable(pager, np => routes.Team.members(t.slug, np).url)
          )
        )
      )
    }
}

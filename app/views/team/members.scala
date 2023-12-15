package views.html.team

import controllers.routes
import controllers.team.routes.{ Team as teamRoutes }

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.*
import lila.common.paginator.Paginator
import lila.team.{ Team, TeamMember }

object members:

  import trans.team.*

  def apply(t: Team, pager: Paginator[TeamMember.UserAndDate])(using PageContext) =
    bits.layout(
      title = t.name,
      openGraph = lila.app.ui
        .OpenGraph(
          title = s"${t.name} • ${trans.team.teamRecentMembers.txt()}",
          url = s"$netBaseUrl${teamRoutes.show(t.id).url}",
          description = t.intro so { shorten(_, 152) }
        )
        .some
    ):
      main(cls := "page-small box")(
        boxTop(
          h1(
            teamLink(t, true),
            " • ",
            nbMembers.plural(t.nbMembers, t.nbMembers.localize)
          )
        ),
        table(cls := "team-members slist slist-pad"):
          tbody(cls := "infinite-scroll")(
            pager.currentPageResults.map { case TeamMember.UserAndDate(u, date) =>
              tr(cls := "paginated")(
                td(lightUserLink(u)),
                td(momentFromNowOnce(date))
              )
            },
            pagerNextTable(pager, np => teamRoutes.members(t.slug, np).url)
          )
      )

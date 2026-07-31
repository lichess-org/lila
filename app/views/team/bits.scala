package views.team

import scalalib.paginator.Paginator

import lila.app.UiEnv.{ *, given }

private lazy val bits = lila.team.ui.TeamUi(helpers, env.memo.markdown)
export bits.{ list, membersPage }
lazy val form = lila.team.ui.FormUi(helpers, bits)(views.captcha.apply)
lazy val request = lila.team.ui.RequestUi(helpers, bits)
lazy val update = lila.team.ui.TeamUpdateUi(helpers)
lazy val admin = lila.team.ui.TeamAdminUi(helpers, bits)

def updateEventLinks(
    tours: List[lila.tournament.Tournament],
    swiss: List[lila.swiss.Swiss]
)(using Context): List[(Tag, Instant, Call)] =
  tours.map(t => (views.tournament.ui.tournamentLink(t), t.startsAt, routes.Tournament.show(t.id)))
    ++ swiss.map(s => (views.swiss.ui.link(s), s.startsAt, routes.Swiss.show(s.id)))

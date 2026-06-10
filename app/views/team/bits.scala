package views.team

import play.api.data.Form
import scalalib.paginator.Paginator

import lila.app.UiEnv.{ *, given }

private lazy val bits = lila.team.ui.TeamUi(helpers, env.memo.markdown)
export bits.{ list, membersPage }
lazy val form = lila.team.ui.FormUi(helpers, bits)(views.captcha.apply)
lazy val request = lila.team.ui.RequestUi(helpers, bits)

object admin:
  private lazy val adminUi = lila.team.ui.AdminUi(helpers, bits)
  export adminUi.{ leaders, kick }

  def pmAll(
      t: lila.team.Team,
      form: Form[?],
      tours: List[lila.tournament.Tournament],
      swiss: List[lila.swiss.Swiss],
      unsubs: Int,
      limiter: (Int, Instant)
  )(using Context) =

    val links: List[(Tag, Instant, Call)] =
      tours.map(t => (views.tournament.ui.tournamentLink(t), t.startsAt, routes.Tournament.show(t.id)))
        ++ swiss.map(s => (views.swiss.ui.link(s), s.startsAt, routes.Swiss.show(s.id)))

    val toursFrag = links.nonEmpty.option:
      div(cls := "tournaments")(
        p(trans.team.youWayWantToLinkOneOfTheseTournaments()),
        p:
          ul:
            links.map: (link, startsAt, call) =>
              li(
                link,
                " ",
                momentFromNow(startsAt),
                " ",
                a(dataIcon := Icon.Forward, cls := "text copy-url-button", data.copyurl := routeUrl(call))
              )
        ,
        br
      )
    adminUi.pmAll(t, form, toursFrag, unsubs, limiter, lila.team.TeamLimiter.pmAllCredits)

package views.team

import play.api.data.Form
import scalalib.paginator.Paginator

import lila.app.UiEnv.{ *, given }

private lazy val bits = lila.team.ui.TeamUi(helpers)(using env.executor)
export bits.{ list, membersPage }
lazy val form    = lila.team.ui.FormUi(helpers, bits)(views.captcha.apply)
lazy val request = lila.team.ui.RequestUi(helpers, bits)

object admin:
  private lazy val adminUi = lila.team.ui.AdminUi(helpers, bits)
  export adminUi.{ leaders, kick }

  def pmAll(
      t: lila.team.Team,
      form: Form[?],
      tours: List[lila.tournament.Tournament],
      unsubs: Int,
      limiter: (Int, Instant)
  )(using Context) =
    val toursFrag = tours.nonEmpty.option:
      div(cls := "tournaments")(
        p(trans.team.youWayWantToLinkOneOfTheseTournaments()),
        p:
          ul:
            tours.map: t =>
              li(
                views.tournament.ui.tournamentLink(t),
                " ",
                momentFromNow(t.startsAt),
                " ",
                a(
                  dataIcon     := Icon.Forward,
                  cls          := "text copy-url-button",
                  data.copyurl := s"${netConfig.domain}${routes.Tournament.show(t.id).url}"
                )
              )
        ,
        br
      )
    adminUi.pmAll(t, form, toursFrag, unsubs, limiter, lila.app.mashup.TeamInfo.pmAllCredits)

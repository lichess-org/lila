package views.html.tournament

import controllers.routes
import play.api.i18n.Lang

import lila.app.templating.Environment.{ *, given }
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.rating.PerfType

object leaderboard:

  private def freqWinner(w: lila.tournament.Winner, freq: String)(using Translate) =
    li(
      userIdLink(w.userId.some),
      a(title := w.tourName, href := routes.Tournament.show(w.tourId))(freq)
    )

  private val section = st.section(cls := "tournament-leaderboards__item")

  private def freqWinners(fws: lila.tournament.FreqWinners, perfType: PerfType, name: String)(using
      Translate
  ) =
    section(
      h2(cls := "text", dataIcon := perfType.icon)(name),
      ul(
        fws.yearly.map: w =>
          freqWinner(w, "Yearly"),
        fws.monthly.map: w =>
          freqWinner(w, "Monthly"),
        fws.weekly.map: w =>
          freqWinner(w, "Weekly"),
        fws.daily.map: w =>
          freqWinner(w, "Daily")
      )
    )

  def apply(winners: lila.tournament.AllWinners)(using PageContext) =
    views.html.base.layout(
      title = "Tournament leaderboard",
      moreCss = cssTag("tournament.leaderboard"),
      wrapClass = "full-screen-force"
    ) {
      def eliteWinners =
        section(
          h2(cls := "text", dataIcon := Icon.CrownElite)("Elite Arena"),
          ul(
            winners.elite.map { w =>
              li(
                userIdLink(w.userId.some),
                a(title := w.tourName, href := routes.Tournament.show(w.tourId))(showDate(w.date))
              )
            }
          )
        )

      def marathonWinners =
        section(
          h2(cls := "text", dataIcon := Icon.Globe)("Marathon"),
          ul(
            winners.marathon.map { w =>
              li(
                userIdLink(w.userId.some),
                a(title := w.tourName, href := routes.Tournament.show(w.tourId))(
                  w.tourName.replace(" Marathon", "")
                )
              )
            }
          )
        )
      main(cls := "page-menu")(
        views.html.user.bits.communityMenu("tournament"),
        div(cls := "page-menu__content box box-pad")(
          h1(cls := "box__top")(trans.arena.tournamentWinners()),
          div(cls := "tournament-leaderboards")(
            eliteWinners,
            freqWinners(winners.hyperbullet, PerfType.Bullet, "HyperBullet"),
            freqWinners(winners.bullet, PerfType.Bullet, "Bullet"),
            freqWinners(winners.superblitz, PerfType.Blitz, "SuperBlitz"),
            freqWinners(winners.blitz, PerfType.Blitz, "Blitz"),
            freqWinners(winners.rapid, PerfType.Rapid, "Rapid"),
            marathonWinners,
            lila.tournament.WinnersApi.variants.map: v =>
              PerfKey.byVariant(v).map { pk =>
                winners.variants.get(chess.variant.Variant.LilaKey(pk.value)).map {
                  freqWinners(_, pk, v.name)
                }
              }
          )
        )
      )
    }

package views.html.tournament

import play.api.i18n.Lang

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.rating.PerfType

import controllers.routes

object leaderboard {

  private def freqWinner(w: lila.tournament.Winner, freq: String)(implicit lang: Lang) =
    li(
      userIdLink(w.userId.some),
      a(title := w.tourName, href := routes.Tournament.show(w.tourId))(freq)
    )

  private val section = st.section(cls := "tournament-leaderboards__item")

  private def freqWinners(fws: lila.tournament.FreqWinners, perfType: PerfType, name: String)(implicit
      lang: Lang
  ) =
    section(
      h2(cls := "text", dataIcon := perfType.iconChar)(name),
      ul(
        fws.yearly.map { w =>
          freqWinner(w, "Yearly")
        },
        fws.monthly.map { w =>
          freqWinner(w, "Monthly")
        },
        fws.weekly.map { w =>
          freqWinner(w, "Weekly")
        },
        fws.daily.map { w =>
          freqWinner(w, "Daily")
        }
      )
    )

  def apply(winners: lila.tournament.AllWinners)(implicit ctx: Context) =
    views.html.base.layout(
      title = "Tournament leaderboard",
      moreCss = cssTag("tournament.leaderboard"),
      wrapClass = "full-screen-force"
    ) {
      def eliteWinners =
        section(
          h2(cls := "text", dataIcon := "")("Elite Arena"),
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
          h2(cls := "text", dataIcon := "")("Marathon"),
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
          h1("Tournament winners"),
          div(cls := "tournament-leaderboards")(
            eliteWinners,
            freqWinners(winners.hyperbullet, PerfType.Bullet, "HyperBullet"),
            freqWinners(winners.bullet, PerfType.Bullet, "Bullet"),
            freqWinners(winners.superblitz, PerfType.Blitz, "SuperBlitz"),
            freqWinners(winners.blitz, PerfType.Blitz, "Blitz"),
            freqWinners(winners.rapid, PerfType.Rapid, "Rapid"),
            marathonWinners,
            lila.tournament.WinnersApi.variants.map { v =>
              PerfType.byVariant(v).map { pt =>
                winners.variants.get(pt.key).map { w =>
                  freqWinners(w, pt, v.name)
                }
              }
            }
          )
        )
      )
    }
}

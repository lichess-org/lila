package views.html.tournament

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.rating.PerfType
import lila.tournament.Tournament

import controllers.routes

object shields {

  private val section = st.section(cls := "tournament-shields__item")

  def apply(history: lila.tournament.TournamentShield.History)(implicit ctx: Context) =
    views.html.base.layout(
      title = "Tournament shields",
      moreCss = responsiveCssTag("tournament.leaderboard"),
      responsive = true,
      wrapClass = "full-screen-force"
    ) {
        main(cls := "page-menu")(
          views.html.user.bits.communityMenu("shield"),
          div(cls := "page-menu__content box box-pad")(
            h1("Tournament shields"),
            div(cls := "tournament-shields")(
              history.sorted.map {
                case (categ, awards) => {
                  section(
                    h2(
                      span(cls := "shield_trophy")(categ.iconChar.toString),
                      categ.name
                    ),
                    ul(
                      awards.map { aw =>
                        li(
                          userIdLink(aw.owner.value.some),
                          a(href := routes.Tournament.show(aw.tourId))(showDate(aw.date))
                        )
                      }
                    )
                  )
                }
              }
            )
          )
        )
      }
}

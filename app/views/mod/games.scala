package views.html.mod

import controllers.GameMod
import scala.util.chaining._
import controllers.routes
import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.evaluation.PlayerAssessment
import lila.game.Pov
import lila.tournament.Tournament
import lila.user.User
import lila.tournament.LeaderboardApi.TourEntry
import lila.swiss.Swiss

object games {

  private val sortNumberTh = th(attr("data-sort-method") := "number")
  private val sortNoneTh   = th(attr("data-sort-method") := "none")
  private val dataSort     = attr("data-sort")

  def apply(
      user: User,
      filterForm: Form[GameMod.Filter],
      games: List[(Pov, Either[PlayerAssessment, PlayerAssessment.Basics])],
      arenas: Seq[TourEntry],
      swisses: Seq[(Swiss.IdName, Int)]
  )(implicit
      ctx: Context
  ) =
    views.html.base.layout(
      title = s"${user.username} games",
      moreCss = cssTag("mod.games"),
      moreJs = jsModule("mod.games")
    ) {
      main(cls := "mod-games box")(
        div(cls := "box__top")(
          h1(userLink(user), " games (WIP)"),
          div(cls := "box__top__actions")(
            form(method := "get", action := routes.GameMod.index(user.id), cls := "mod-games__filter-form")(
              form3.select(
                filterForm("arena"),
                arenas.map(t =>
                  t.tour.id -> List(
                    s"games ${t.entry.nbGames}",
                    s"rank ${t.entry.rank}",
                    s"top ${t.entry.rankRatio.percent}%",
                    t.tour.name()
                  ).mkString(" / ")
                ),
                pluralize("recent arena", arenas.size).some,
                disabled = arenas.isEmpty
              ),
              form3.select(
                filterForm("swiss"),
                swisses.map { case (swiss, rank) =>
                  swiss.id.value -> s"rank ${rank} / ${swiss.name}"
                },
                s"${swisses.size} recent swiss".some,
                disabled = swisses.isEmpty
              )
            )
          )
        ),
        postForm(action := routes.Analyse.multipleAnalysis(user.id), cls := "mod-games__analysis-form")(
          submitButton(cls := "button button-empty button-thin")("Analyse selected"),
          table(cls := "mod-games game-list slist")(
            thead(
              tr(
                sortNoneTh(input(tpe := "checkbox", name := s"game[]", st.value := "all")),
                sortNumberTh("Opponent"),
                sortNumberTh("Perf"),
                th(iconTag('g')),
                sortNumberTh("Moves"),
                sortNumberTh("Result"),
                sortNumberTh("ACPL", br, "(Avg ± SD)"),
                sortNumberTh("Times", br, "(Avg ± SD)"),
                sortNumberTh("Blur"),
                sortNumberTh("Date")
              )
            ),
            tbody(
              games.map { case (pov, assessment) =>
                tr(
                  td(
                    input(
                      tpe := "checkbox",
                      name := s"game[]",
                      st.value := pov.gameId
                    )
                  ),
                  td(dataSort := ~pov.opponent.rating)(
                    playerLink(pov.opponent, withDiff = false)
                  ),
                  td(dataSort := pov.game.clock.fold(Int.MaxValue)(_.config.estimateTotalSeconds))(
                    pov.game.perfType.map { pt =>
                      iconTag(pt.iconChar)(cls := "text")
                    },
                    shortClockName(pov.game.clock.map(_.config))
                  ),
                  td(dataSort := ~pov.game.tournamentId)(
                    pov.game.tournamentId map { tourId =>
                      a(
                        dataIcon := "g",
                        href := routes.Tournament.show(tourId).url,
                        title := tournamentIdToName(tourId)
                      )
                    },
                    pov.game.swissId map { swissId =>
                      a(
                        dataIcon := "g",
                        href := routes.Swiss.show(swissId).url,
                        title := s"Swiss #${swissId}"
                      )
                    }
                  ),
                  td(dataSort := pov.game.playedTurns)(pov.game.playedTurns),
                  td(dataSort := ~pov.player.ratingDiff)(
                    pov.win match {
                      case Some(true)  => goodTag(cls := "result")("1")
                      case Some(false) => badTag(cls := "result")("0")
                      case None        => span(cls := "result")("½")
                    },
                    pov.player.ratingDiff match {
                      case Some(d) if d > 0 => goodTag(s"+$d")
                      case Some(d) if d < 0 => badTag(d)
                      case _                => span("-")
                    }
                  ),
                  assessment match {
                    case Left(full) => td(dataSort := full.analysis.avg)(full.analysis.toString)
                    case _          => td
                  },
                  assessment.fold(_.basics, identity) pipe { basics =>
                    frag(
                      td(dataSort := basics.moveTimes.avg)(
                        s"${basics.moveTimes / 10}",
                        basics.mtStreak ?? frag(br, "streak")
                      ),
                      td(dataSort := basics.blurs)(
                        s"${basics.blurs}%",
                        basics.blurStreak.filter(8 <=) map { s =>
                          frag(br, s"streak $s/12")
                        }
                      )
                    )
                  },
                  td(dataSort := pov.game.movedAt.getSeconds.toString)(
                    a(href := routes.Round.watcher(pov.gameId, pov.color.name))(
                      momentFromNowServer(pov.game.movedAt)
                    )
                  )
                )
              }
            )
          )
        )
      )
    }
}

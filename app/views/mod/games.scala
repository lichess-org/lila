package views.html.mod

import controllers.GameMod
import controllers.routes
import play.api.data.Form
import scala.util.chaining.*

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.evaluation.PlayerAssessment
import lila.game.Pov
import lila.rating.PerfType
import lila.swiss.Swiss
import lila.tournament.LeaderboardApi.TourEntry
import lila.user.User
import views.html.mod.userTable.sortNoneTh

object games:

  def apply(
      user: User,
      filterForm: Form[GameMod.Filter],
      games: Either[List[Pov], List[(Pov, Either[PlayerAssessment, PlayerAssessment.Basics])]],
      arenas: Seq[TourEntry],
      swisses: Seq[(Swiss.IdName, Rank)]
  )(using PageContext) =
    views.html.base.layout(
      title = s"${user.username} games",
      moreCss = cssTag("mod.games"),
      moreJs = jsModule("mod.games")
    ) {
      main(cls := "mod-games box")(
        boxTop(
          h1(userLink(user, params = "?mod"), " games"),
          div(cls := "box__top__actions")(
            form(method := "get", action := routes.GameMod.index(user.id), cls := "mod-games__filter-form")(
              form3.input(filterForm("opponents"))(placeholder  := "Opponents"),
              form3.input(filterForm("nbGamesOpt"))(placeholder := "Nb games"),
              form3.select(
                filterForm("perf"),
                PerfType.nonPuzzle.map: p =>
                  p.key -> p.trans,
                "Variant".some
              ),
              form3.select(
                filterForm("arena"),
                arenas.map: t =>
                  t.tour.id -> List(
                    s"games ${t.entry.nbGames}",
                    s"rank ${t.entry.rank}",
                    s"top ${t.entry.rankRatio.percent}%",
                    t.tour.name()
                  ).mkString(" / "),
                pluralize("arena", arenas.size).some,
                disabled = arenas.isEmpty
              ),
              form3.select(
                filterForm("swiss"),
                swisses.map: (swiss, rank) =>
                  swiss.id -> s"rank ${rank} / ${swiss.name}",
                s"${swisses.size} swiss".some,
                disabled = swisses.isEmpty
              )
            )
          )
        ),
        postForm(action := routes.GameMod.post(user.id), cls := "mod-games__analysis-form")(
          isGranted(_.UserEvaluate) option submitButton(
            cls   := "button button-empty button-thin",
            name  := "action",
            value := "analyse"
          )("Analyse selected"),
          submitButton(cls := "button button-empty button-thin", name := "action", value := "pgn")(
            "Download PGN"
          ),
          table(cls := "mod-games game-list slist")(
            thead(
              tr(
                sortNoneTh(
                  input(
                    tpe      := "checkbox",
                    name     := "game[]",
                    st.value := "all"
                  )
                ),
                thSortNumber("Opponent"),
                thSortNumber("Speed"),
                th(iconTag(licon.Trophy)),
                thSortNumber("Moves"),
                thSortNumber("Result"),
                thSortNumber("ACPL", br, "(Avg ± SD)"),
                thSortNumber("Times", br, "(Avg ± SD)"),
                thSortNumber("Blur"),
                thSortNumber(dataSortDefault)("Date")
              )
            ),
            tbody(
              games.fold(_.map(_ -> None), _.map { case (pov, ass) => pov -> Some(ass) }).map {
                case (pov, assessment) =>
                  tr(
                    td(cls := pov.game.analysable.option("input"))(
                      pov.game.analysable option input(
                        tpe      := "checkbox",
                        name     := s"game[]",
                        st.value := pov.gameId
                      )
                    ),
                    td(dataSort := pov.opponent.rating.fold(0)(_.value))(
                      playerLink(pov.opponent, withDiff = false)
                    ),
                    td(
                      dataSort := pov.game.clock.fold(
                        pov.game.correspondenceClock.fold(Int.MaxValue)(_.daysPerTurn * 3600 * 24)
                      )(_.config.estimateTotalSeconds)
                    )(
                      iconTag(pov.game.perfType.icon)(cls := "text"),
                      shortClockName(pov.game)
                    ),
                    td(dataSort := pov.game.tournamentId.so(_.value))(
                      pov.game.tournamentId map { tourId =>
                        a(
                          dataIcon := licon.Trophy,
                          href     := routes.Tournament.show(tourId).url,
                          title    := tournamentIdToName(tourId)
                        )
                      },
                      pov.game.swissId map { swissId =>
                        a(
                          dataIcon := licon.Trophy,
                          href     := routes.Swiss.show(swissId).url,
                          title    := s"Swiss #${swissId}"
                        )
                      }
                    ),
                    td(dataSort := pov.moves)(pov.moves),
                    td(dataSort := ~pov.player.ratingDiff)(
                      pov.win match
                        case Some(true)  => goodTag(cls := "result")("1")
                        case Some(false) => badTag(cls := "result")("0")
                        case None        => span(cls := "result")("½")
                      ,
                      pov.player.ratingDiff match
                        case Some(d) if d > 0 => goodTag(s"+$d")
                        case Some(d) if d < 0 => badTag(d)
                        case _                => span("-")
                    ),
                    assessment match
                      case Some(Left(full)) => td(dataSort := full.analysis.avg)(full.analysis.toString)
                      case _                => td
                    ,
                    assessment match
                      case Some(ass) =>
                        ass.fold(_.basics, identity) pipe { basics =>
                          frag(
                            td(dataSort := basics.moveTimes.sd)(
                              s"${basics.moveTimes / 10}",
                              basics.mtStreak so frag(br, "streak")
                            ),
                            td(dataSort := basics.blurs)(
                              s"${basics.blurs}%",
                              basics.blurStreak.filter(8 <=) map { s =>
                                frag(br, s"streak $s/12")
                              }
                            )
                          )
                        }
                      case _ => frag(td, td)
                    ,
                    td(dataSort := pov.game.movedAt.toSeconds.toString)(
                      a(href := routes.Round.watcher(pov.gameId, pov.color.name), cls := "glpt")(
                        momentFromNowServerText(pov.game.movedAt)
                      )
                    )
                  )
              }
            )
          )
        )
      )
    }

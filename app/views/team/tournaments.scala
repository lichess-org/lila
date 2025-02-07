package views.team

import lila.app.UiEnv.{ *, given }
import lila.app.mashup.TeamInfo

// both arena and swiss
object tournaments:

  def page(t: lila.team.Team, tours: TeamInfo.PastAndNext)(using Context) =
    Page(s"${t.name} • ${trans.site.tournaments.txt()}")
      .graph(
        title = s"${t.name} team tournaments",
        url = s"$netBaseUrl${routes.Team.tournaments(t.id)}",
        description = shorten(t.description.value, 152)
      )
      .css("bits.team")
      .flag(_.fullScreen):
        main(
          div(cls := "box")(
            boxTop:
              h1(teamLink(t, true), " • ", trans.site.tournaments())
            ,
            div(cls := "team-events team-tournaments team-tournaments--both")(
              div(cls := "team-tournaments__next")(
                h2(trans.team.upcomingTournaments()),
                table(cls := "slist slist-pad slist-invert")(
                  renderList(tours.next)
                )
              ),
              div(cls := "team-tournaments__past")(
                h2(trans.team.completedTourns()),
                table(cls := "slist slist-pad")(
                  renderList(tours.past)
                )
              )
            )
          )
        )

  def renderList(tours: List[TeamInfo.AnyTour])(using Context) =
    tbody:
      tours.map: any =>
        tr(
          cls := List(
            "enterable" -> any.isEnterable,
            "soon"      -> any.isNowOrSoon
          )
        )(
          td(cls := "icon")(
            iconTag(any.value.fold(views.tournament.ui.tournamentIcon, _.perfType.icon))
          ),
          td(cls := "header")(
            any.value.fold(
              t =>
                a(href := routes.Tournament.show(t.id))(
                  span(cls := "name")(t.name()),
                  span(cls := "setup")(
                    t.clock.show,
                    " • ",
                    if t.variant.exotic then t.variant.name else t.perfType.trans,
                    t.position.isDefined.option(frag(" • ", trans.site.thematic())),
                    " • ",
                    if t.mode.rated then trans.site.ratedTournament() else trans.site.casualTournament(),
                    " • ",
                    t.durationString
                  )
                ),
              s =>
                a(href := routes.Swiss.show(s.id))(
                  span(cls := "name")(s.name),
                  span(cls := "setup")(
                    s.clock.show,
                    " • ",
                    if s.variant.exotic then s.variant.name else s.perfType.trans,
                    " • ",
                    (if s.settings.rated then trans.site.ratedTournament else trans.site.casualTournament) ()
                  )
                )
            )
          ),
          td(cls := "infos")(
            any.value.fold(
              t =>
                frag(
                  t.teamBattle.fold(trans.team.innerTeam()): battle =>
                    trans.team.battleOfNbTeams.plural(battle.teams.size, battle.teams.size.localize),
                  br,
                  renderStartsAt(any)
                ),
              s =>
                frag(
                  trans.swiss.xRoundsSwiss.plural(s.settings.nbRounds, s.settings.nbRounds.localize),
                  br,
                  renderStartsAt(any)
                )
            )
          ),
          td(cls := "text", dataIcon := Icon.User)(any.nbPlayers.localize)
        )

  private def renderStartsAt(any: TeamInfo.AnyTour)(using Translate): Frag =
    if any.isEnterable && any.startsAt.isBeforeNow then trans.site.playingRightNow()
    else momentFromNowOnce(any.startsAt)

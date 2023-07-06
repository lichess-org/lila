package views.html
package game

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

import controllers.routes

object side:

  private val separator  = " • "
  private val dataUserTv = attr("data-user-tv")
  private val dataTime   = attr("data-time")

  def apply(
      pov: lila.game.Pov,
      initialFen: Option[chess.format.Fen.Epd],
      tour: Option[lila.tournament.TourAndTeamVs],
      simul: Option[lila.simul.Simul],
      userTv: Option[lila.user.User] = None,
      bookmarked: Boolean
  )(using ctx: Context): Option[Frag] =
    ctx.noBlind option frag(
      meta(pov, initialFen, tour, simul, userTv, bookmarked),
      pov.game.userIds.filter(isStreaming) map views.html.streamer.bits.contextual
    )

  def meta(
      pov: lila.game.Pov,
      initialFen: Option[chess.format.Fen.Epd],
      tour: Option[lila.tournament.TourAndTeamVs],
      simul: Option[lila.simul.Simul],
      userTv: Option[lila.user.User] = None,
      bookmarked: Boolean
  )(using ctx: Context): Option[Frag] =
    ctx.noBlind option {
      import pov.*
      div(cls := "game__meta")(
        st.section(
          div(cls := "game__meta__infos", dataIcon := bits.gameIcon(game))(
            div(
              div(cls := "header")(
                div(cls := "setup")(
                  views.html.bookmark.toggle(game, bookmarked),
                  if game.imported then
                    div(
                      a(href := routes.Importer.importGame, title := trans.importGame.txt())("IMPORT"),
                      separator,
                      bits.variantLink(game.variant, game.perfType, initialFen = initialFen, shortName = true)
                    )
                  else
                    frag(
                      widgets showClock game,
                      separator,
                      (if game.rated then trans.rated else trans.casual).txt(),
                      separator,
                      bits.variantLink(game.variant, game.perfType, initialFen, shortName = true)
                    )
                ),
                game.pgnImport.flatMap(_.date).fold(momentFromNowWithPreload(game.createdAt))(frag(_))
              ),
              game.pgnImport
                .flatMap(_.user)
                .map: importedBy =>
                  small(
                    trans.importedByX(userIdLink(importedBy.some, None, withOnline = false)),
                    ctx.is(importedBy) option
                      form(cls := "delete", method := "post", action := routes.Game.delete(game.id)):
                        submitButton(
                          cls   := "button-link confirm",
                          title := trans.deleteThisImportedGame.txt()
                        )(trans.delete.txt())
                  )
            )
          ),
          div(cls := "game__meta__players")(
            game.players.mapList: p =>
              frag(
                div(cls := s"player color-icon is ${p.color.name} text")(
                  playerLink(p, withOnline = false, withDiff = true, withBerserk = true)
                ),
                tour.flatMap(_.teamVs).map(_.teams(p.color)) map {
                  teamLink(_, withIcon = false)(cls := "team")
                }
              )
          )
        ),
        game.finishedOrAborted option st.section(cls := "status")(
          gameEndStatus(game),
          game.winner.map: winner =>
            frag(
              separator,
              winner.color.fold(trans.whiteIsVictorious, trans.blackIsVictorious)()
            )
        ),
        game.variant.chess960 so frag:
          chess.variant.Chess960
            .positionNumber(initialFen | chess.format.Fen.initial)
            .map: number =>
              val url = routes.UserAnalysis
                .parseArg(s"chess960/${underscoreFen(initialFen | chess.format.Fen.initial)}")
              st.section(trans.chess960StartPosition(a(href := url)(number)))
        ,
        userTv.map: u =>
          st.section(cls := "game__tv"):
            h2(cls := "top user-tv text", dataUserTv := u.id, dataIcon := licon.AnalogTv)(u.titleUsername)
        ,
        tour
          .map: t =>
            st.section(cls := "game__tournament")(
              a(cls := "text", dataIcon := licon.Trophy, href := routes.Tournament.show(t.tour.id)):
                t.tour.name()
              ,
              div(cls := "clock", dataTime := t.tour.secondsToFinish)(t.tour.clockStatus)
            )
          .orElse:
            game.tournamentId.map: tourId =>
              st.section(cls := "game__tournament-link")(tournamentLink(tourId))
          .orElse:
            game.swissId.map: swissId =>
              st.section(cls := "game__tournament-link"):
                views.html.swiss.bits.link(SwissId(swissId))
          .orElse:
            simul.map: sim =>
              st.section(cls := "game__simul-link"):
                a(href := routes.Simul.show(sim.id))(sim.fullName)
      )
    }

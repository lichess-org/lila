package views.game
package side

import lila.app.UiEnv.{ *, given }
import lila.game.GameExt.perfType

private val separator = " • "
private val dataUserTv = attr("data-user-tv")
private val dataTime = attr("data-time")

def apply(
    pov: Pov,
    initialFen: Option[chess.format.Fen.Full],
    tour: Option[lila.tournament.TourAndTeamVs],
    simul: Option[lila.simul.Simul],
    userTv: Option[User] = None,
    bookmarked: Boolean
)(using ctx: Context): Option[Frag] =
  ctx.noBlind.option:
    frag(
      meta(pov, initialFen, tour, simul, userTv, bookmarked),
      pov.game.userIds.filter(isStreaming).map(views.streamer.bits.contextual)
    )

def meta(
    pov: Pov,
    initialFen: Option[chess.format.Fen.Full],
    tour: Option[lila.tournament.TourAndTeamVs],
    simul: Option[lila.simul.Simul],
    userTv: Option[User] = None,
    bookmarked: Boolean
)(using ctx: Context): Option[Frag] =
  ctx.noBlind.option:
    import pov.*
    div(cls := "game__meta")(
      st.section(
        div(cls := "game__meta__infos", dataIcon := ui.gameIcon(game))(
          div(cls := "header")(
            div(cls := "setup")(
              views.bookmark.toggle(game, bookmarked),
              if game.sourceIs(_.Import) then
                div(
                  a(href := routes.Importer.importGame, title := trans.site.importGame.txt())("IMPORT"),
                  separator,
                  variantLink(game.variant, game.perfType, initialFen = initialFen, shortName = true)
                )
              else
                frag(
                  ui.widgets.showClock(game),
                  separator,
                  ratedName(game.rated),
                  separator,
                  variantLink(game.variant, game.perfType, initialFen, shortName = true)
                )
            ),
            game.pgnImport.flatMap(_.date).fold(momentFromNowWithPreload(game.createdAt))(frag(_))
          ),
          game.pgnImport
            .flatMap(_.user)
            .map: importedBy =>
              small(
                trans.site.importedByX(userIdLink(importedBy.some, None, withOnline = false)),
                ctx
                  .is(importedBy)
                  .option(form(cls := "delete", method := "post", action := routes.Game.delete(game.id)):
                    submitButton(
                      cls := "button-link yes-no-confirm",
                      title := trans.site.deleteThisImportedGame.txt()
                    )(trans.site.delete.txt()))
              )
        ),
        div(cls := "game__meta__players")(
          game.players.mapList: p =>
            frag(
              div(cls := s"player color-icon is ${p.color.name} text")(
                playerLink(
                  p,
                  withOnline = false,
                  withDiff = true,
                  withBerserk = true
                )
              ),
              tour.flatMap(_.teamVs).map(_.teams(p.color)).map {
                teamLink(_, withIcon = false)(cls := "team")
              }
            )
        )
      ),
      game.finishedOrAborted.option(
        st.section(cls := "status")(
          ui.gameEndStatus(game),
          game.winner.map: winner =>
            frag(
              separator,
              winner.color.fold(trans.site.whiteIsVictorious, trans.site.blackIsVictorious)()
            )
        )
      ),
      game.variant.chess960.option:
        chess.variant.Chess960
          .positionNumber(initialFen | chess.format.Fen.initial)
          .map: number =>
            st.section(
              trans.site.chess960StartPosition(
                a(
                  targetBlank,
                  href := "https://chess960.net/wp-content/uploads/2018/02/chess960-starting-positions.pdf"
                )(number)
              )
            )
      ,
      userTv.map: u =>
        st.section(cls := "game__tv"):
          h2(cls := "top user-tv text", dataUserTv := u.id, dataIcon := Icon.AnalogTv)(u.titleUsername)
      ,
      tour
        .map: t =>
          st.section(cls := "game__tournament")(
            a(cls := "text", dataIcon := Icon.Trophy, href := routes.Tournament.show(t.tour.id)):
              t.tour.name()
            ,
            div(cls := "clock", dataTime := t.tour.secondsToFinish)(t.tour.clockStatus)
          )
        .orElse:
          game.tournamentId.map: tourId =>
            st.section(cls := "game__tournament-link")(views.tournament.ui.tournamentLink(tourId))
        .orElse:
          game.swissId.map: swissId =>
            st.section(cls := "game__tournament-link"):
              views.swiss.ui.link(swissId)
        .orElse:
          simul.map: sim =>
            st.section(cls := "game__simul-link"):
              a(href := routes.Simul.show(sim.id))(sim.fullName)
    )

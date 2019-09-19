package views.html
package game

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object side {

  private val separator = " • "
  private val dataUserTv = attr("data-user-tv")
  private val dataTime = attr("data-time")

  def apply(
    pov: lila.game.Pov,
    initialFen: Option[chess.format.FEN],
    tour: Option[lila.tournament.Tournament],
    simul: Option[lila.simul.Simul],
    userTv: Option[lila.user.User] = None,
    bookmarked: Boolean
  )(implicit ctx: Context): Option[Frag] = ctx.noBlind option frag(
    meta(pov, initialFen, tour, simul, userTv, bookmarked),
    pov.game.userIds.filter(isStreaming) map views.html.streamer.bits.contextual
  )

  def meta(
    pov: lila.game.Pov,
    initialFen: Option[chess.format.FEN],
    tour: Option[lila.tournament.Tournament],
    simul: Option[lila.simul.Simul],
    userTv: Option[lila.user.User] = None,
    bookmarked: Boolean
  )(implicit ctx: Context): Option[Frag] = ctx.noBlind option {
    import pov._
    div(cls := "game__meta")(
      st.section(
        div(cls := "game__meta__infos", dataIcon := bits.gameIcon(game))(
          div(
            div(cls := "header")(
              div(cls := "setup")(
                views.html.bookmark.toggle(game, bookmarked),
                if (game.imported) div(
                  a(href := routes.Importer.importGame, title := trans.importGame.txt())("IMPORT"),
                  separator,
                  if (game.variant.exotic)
                    bits.variantLink(game.variant, (if (game.variant == chess.variant.KingOfTheHill) game.variant.shortName else game.variant.name).toUpperCase, initialFen = initialFen)
                  else
                    game.variant.name.toUpperCase
                )
                else frag(
                  widgets showClock game,
                  separator,
                  (if (game.rated) trans.rated else trans.casual).txt(),
                  separator,
                  if (game.variant.exotic)
                    bits.variantLink(game.variant, (if (game.variant == chess.variant.KingOfTheHill) game.variant.shortName else game.variant.name).toUpperCase, initialFen = initialFen)
                  else game.perfType.map { pt =>
                    span(title := pt.title)(pt.shortName)
                  }
                )
              ),
              game.pgnImport.flatMap(_.date).map(frag(_)) getOrElse {
                frag(if (game.isBeingPlayed) trans.playingRightNow() else momentFromNow(game.createdAt))
              }
            ),
            game.pgnImport.flatMap(_.date).map { date =>
              small(
                "Imported ",
                game.pgnImport.flatMap(_.user).map { user =>
                  trans.by(userIdLink(user.some, None, false))
                }
              )
            }
          )
        ),
        div(cls := "game__meta__players")(
          game.players.map { p =>
            div(cls := s"player color-icon is ${p.color.name} text")(
              playerLink(p, withOnline = false, withDiff = true, withBerserk = true)
            )
          }
        )
      ),
      game.finishedOrAborted option {
        st.section(cls := "status")(
          gameEndStatus(game),
          game.winner.map { winner =>
            frag(
              separator,
              winner.color.fold(trans.whiteIsVictorious, trans.blackIsVictorious)()
            )
          }
        )
      },
      initialFen.ifTrue(game.variant.chess960).map(_.value).flatMap {
        chess.variant.Chess960.positionNumber
      }.map { number =>
        st.section(
          "Chess960 start position: ",
          strong(number)
        )
      },

      userTv.map { u =>
        st.section(cls := "game__tv")(
          h2(cls := "top user-tv text", dataUserTv := u.id, dataIcon := "1")(u.titleUsername)
        )
      },

      tour.map { t =>
        st.section(cls := "game__tournament")(
          a(cls := "text", dataIcon := "g", href := routes.Tournament.show(t.id))(t.fullName),
          div(cls := "clock", dataTime := t.secondsToFinish)(div(cls := "time")(t.clockStatus))
        )
      } orElse {
        game.tournamentId map { tourId =>
          st.section(cls := "game__tournament-link")(
            a(href := routes.Tournament.show(tourId), dataIcon := "g", cls := "text")(tournamentIdToName(tourId))
          )
        }
      },

      simul.map { sim =>
        st.section(cls := "game__simul-link")(
          a(href := routes.Simul.show(sim.id))(sim.fullName)
        )
      }
    )
  }
}

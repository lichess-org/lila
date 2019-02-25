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
  )(implicit ctx: Context): Option[Frag] = ctx.noBlind option {
    import pov._
    frag(
      div(cls := "game__meta")(
        div(cls := "game__meta__infos", dataIcon := bits.gameIcon(game))(
          div(cls := "header")(
            div(cls := "setup")(
              views.html.bookmark.toggle(game, bookmarked),
              if (game.imported) frag(
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
                if (game.rated) trans.rated.txt() else trans.casual.txt(),
                separator,
                if (game.variant.exotic)
                  bits.variantLink(game.variant, (if (game.variant == chess.variant.KingOfTheHill) game.variant.shortName else game.variant.name).toUpperCase, initialFen = initialFen)
                else
                  game.perfType.map { pt =>
                    span(title := pt.title)(pt.shortName)
                  }
              )
            ),
            game.pgnImport.flatMap(_.date).map(frag(_)) getOrElse {
              frag(if (game.isBeingPlayed) trans.playingRightNow() else momentFromNow(game.createdAt))
            }
          ),
          game.pgnImport.flatMap(_.date).map { date =>
            frag(
              "Imported",
              game.pgnImport.flatMap(_.user).map { user =>
                frag(
                  " by ",
                  userIdLink(user.some, None, false),
                  br
                )
              }
            )
          }
        ),
        div(cls := "game__meta__players")(
          game.players.map { p =>
            div(cls := s"player color-icon is ${p.color.name} text")(
              playerLink(p, withOnline = false, withDiff = true, withBerserk = true)
            )
          }
        ),
        game.finishedOrAborted option {
          div(cls := "status")(
            gameEndStatus(game),
            game.winner.map { winner =>
              frag(
                separator,
                winner.color.fold(trans.whiteIsVictorious, trans.blackIsVictorious).frag()
              )
            }
          )
        },
        initialFen.ifTrue(game.variant.chess960).map(_.value).flatMap {
          chess.variant.Chess960.positionNumber
        }.map { number =>
          frag(
            "Chess960 start position: ",
            strong(number)
          )
        }
      ),

      game.userIds.filter(isStreaming).map { id =>
        a(cls := "context-streamer text side_box", dataIcon := "", href := routes.Streamer.show(id))(
          usernameOrId(id),
          " is streaming"
        )
      },

      userTv.map { u =>
        div(cls := "side_box")(
          h2(cls := "top user_tv text", dataUserTv := u.id, dataIcon := "1")(u.titleUsername)
        )
      },

      tour.map { t =>
        div(cls := "game__tournament scroll-shadow-soft")(
          p(cls := "top text", dataIcon := "g")(a(href := routes.Tournament.show(t.id))(t.fullName)),
          div(cls := "clock", dataTime := t.secondsToFinish)(
            div(cls := "time")(t.clockStatus)
          )
        )
      } orElse {
        game.tournamentId map { tourId =>
          div(cls := "game__tournament-link")(
            a(href := routes.Tournament.show(tourId), dataIcon := "g", cls := "text")(tournamentIdToName(tourId))
          )
        }
      },

      simul.map { sim =>
        div(cls := "game__simul-link")(
          a(href := routes.Simul.show(sim.id), dataIcon := "|", cls := "text")(sim.fullName)
        )
      }
    )
  }
}

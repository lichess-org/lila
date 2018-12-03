package views.html
package game

import scalatags.Text.all._

import lila.api.Context
import lila.app.templating.Environment._
import lila.i18n.{ I18nKeys => trans }

import controllers.routes

object side {

  private val separator = " • "

  def apply(
    pov: lila.game.Pov,
    initialFen: Option[chess.format.FEN],
    tour: Option[lila.tournament.Tournament],
    simul: Option[lila.simul.Simul],
    userTv: Option[lila.user.User] = None,
    bookmarked: Boolean
  )(implicit ctx: Context) = {
    import pov._
    div(cls := "side")(
      div(cls := "side_box padded")(
        div(cls := "game_infos", dataIcon := bits.gameIcon(game))(
          div(cls := "header")(
            span(cls := "setup")(
              views.html.bookmark.toggle(game, bookmarked),
              if (game.imported) frag(
                a(cls := "hint--top", href := routes.Importer.importGame, dataHint := trans.importGame.txt())("IMPORT"),
                separator,
                if (game.variant.exotic)
                  variantLink(game.variant, (if (game.variant == chess.variant.KingOfTheHill) game.variant.shortName else game.variant.name).toUpperCase, cssClass = "hint--top", initialFen = initialFen)
                else
                  game.variant.name.toUpperCase
              )
              else frag(
                game.clock.map { clock =>
                  frag(clock.config.show)
                } getOrElse {
                  game.daysPerTurn.map { days =>
                    span(cls := "hint--top", dataHint := trans.correspondence.txt())(
                      if (days == 1) trans.oneDay() else trans.nbDays.pluralSame(days)
                    )
                  }.getOrElse {
                    span(cls := "hint--top", dataHint := trans.unlimited.txt())("∞")
                  }
                },
                separator,
                if (game.rated) trans.rated.txt() else trans.casual.txt(),
                separator,
                if (game.variant.exotic)
                  variantLink(game.variant, (if (game.variant == chess.variant.KingOfTheHill) game.variant.shortName else game.variant.name).toUpperCase, cssClass = "hint--top", initialFen = initialFen)
                else
                  game.perfType.map { pt =>
                    span(cls := "hint--top", dataHint := pt.title)(pt.shortName)
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
        div(cls := "players")(
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
                winner.color.fold(trans.whiteIsVictorious(), trans.blackIsVictorious())
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
          h2(cls := "top user_tv text", attr("data-user-tv") := u.id, dataIcon := "1")(u.titleUsername)
        )
      } orElse {
        lila.common.HTTPRequest.isMobile(ctx.req) option
          a(cls := "side_box text deep_link", dataIcon := "", href := "lichess://analyse/@pov.gameId")(
            "Open with ",
            strong("Mobile app")
          )
      },

      tour.map { t =>
        div(cls := "game_tournament side_box no_padding scroll-shadow-soft")(
          p(cls := "top text", dataIcon := "g")(a(href := routes.Tournament.show(t.id))(t.fullName)),
          div(cls := "clock", attr("data-time") := t.secondsToFinish)(
            div(cls := "time")(t.clockStatus)
          )
        )
      } orElse {
        game.tournamentId map { tourId =>
          div(cls := "game_tournament side_box no_padding")(
            p(cls := "top text", dataIcon := "g")(a(href := routes.Tournament.show(tourId))(tournamentIdToName(tourId)))
          )
        }
      },

      simul.map { sim =>
        div(cls := "game_simul side_box no_padding")(
          p(cls := "top text", dataIcon := "|")(a(href := routes.Simul.show(sim.id))(sim.fullName))
        )
      }
    )
  }
}

package views.html
package game

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object side {

  private val separator = " • "
  private val dataUserTv = attr("data-user-tv")
  private val dataTime = attr("data-time")

  def apply(
    pov: lidraughts.game.Pov,
    initialFen: Option[draughts.format.FEN],
    tour: Option[lidraughts.tournament.Tournament],
    simul: Option[lidraughts.simul.Simul],
    userTv: Option[lidraughts.user.User] = None,
    bookmarked: Boolean
  )(implicit ctx: Context): Option[Frag] = ctx.noBlind option {
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
                  bits.variantLink(game.variant, game.variant.name.toUpperCase, cssClass = "hint--top", initialFen = initialFen)
                else
                  game.variant.name.toUpperCase
              )
              else frag(
                widgets showClock game,
                separator,
                if (game.rated) trans.rated.txt() else trans.casual.txt(),
                separator,
                if (game.variant.exotic)
                  bits.variantLink(game.variant, game.variant.name.toUpperCase, cssClass = "hint--top", initialFen = initialFen)
                else
                  game.perfType.map { pt =>
                    span(cls := "hint--top", dataHint := pt.title)(pt.shortName)
                  }
              )
            ),
            game.pdnImport.flatMap(_.date).map(frag(_)) getOrElse {
              frag(if (game.isBeingPlayed) trans.playingRightNow() else momentFromNow(game.createdAt))
            }
          ),
          game.pdnImport.flatMap(_.date).map { date =>
            frag(
              "Imported",
              game.pdnImport.flatMap(_.user).map { user =>
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
                (game.status != draughts.Status.Mate).option(separator),
                winner.color.fold(trans.whiteIsVictorious(), trans.blackIsVictorious())
              )
            }
          )
        }
      ),

      /*game.userIds.filter(isStreaming).map { id =>
        a(cls := "context-streamer text side_box", dataIcon := "", href := routes.Streamer.show(id))(
          usernameOrId(id),
          " is streaming"
        )
      },*/

      userTv.map { u =>
        div(cls := "side_box")(
          h2(cls := "top user_tv text", dataUserTv := u.id, dataIcon := "1")(u.titleUsername)
        )
      } orElse {
        lidraughts.common.HTTPRequest.isMobile(ctx.req) option
          a(cls := "side_box text deep_link", dataIcon := "", href := s"lidraughts://analyse/${pov.gameId}")(
            "Open with ",
            strong("Mobile app")
          )
      },

      tour.map { t =>
        div(cls := "game_tournament side_box no_padding scroll-shadow-soft")(
          p(cls := "top text", dataIcon := "g")(a(href := routes.Tournament.show(t.id))(t.fullName)),
          div(cls := "clock", dataTime := t.secondsToFinish)(
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
          p(cls := "top text", dataIcon := "|")(a(href := routes.Simul.show(sim.id))(sim.fullName)),
          div(cls := "padded")(
            div(cls := "simul_infos")(
              game.playerByUserId(sim.hostId).map { p =>
                frag(
                  playerLink(p, withOnline = false, withRating = false, withDiff = false),
                  " vs. ",
                  trans.nbOpponents(sim.pairings.length)
                )
              },
              br,
              if (sim.isFinished) trans.simulFinished() else trans.nbGamesOngoing(sim.ongoing)
            ),
            sim.targetPct.ifTrue(!sim.isFinished).flatMap { target =>
              frag(
                trans.targetWinningPercentage(s"$target%"), br,
                trans.currentWinningPercentage(span(cls := s"simul_pct_${sim.id}")(if (sim.finished == 0) "-" else sim.winningPercentageStr)), br,
                trans.relativeScoreRequired(span(cls := s"simul_rel_${sim.id}")(sim.relativeScoreStr(ctx.pref.draughtsResult)))
              ).some
            },
            (sim.isFinished && sim.hasFmjd).option {
              game.playerByUserId(sim.hostId).flatMap { p =>
                sim.hostOfficialRating.map { r =>
                  frag(
                    playerLink(p, withOnline = false, withRating = false, withDiff = false),
                    s"$r FMJD"
                  )
                }
              }
              game.opponentByUserId(sim.hostId).flatMap { p =>
                p.userId.flatMap(id => sim.pairings.find(_ is id)).flatMap(_.player.officialRating).map { r =>
                  frag(
                    playerLink(p, withOnline = false, withRating = false, withDiff = false),
                    s"$r FMJD"
                  )
                }
              }
            }
          )
        )
      }
    )
  }
}

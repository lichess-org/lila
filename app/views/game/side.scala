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
  )(implicit ctx: Context): Option[Frag] = ctx.noBlind option frag(
    meta(pov, initialFen, tour, simul, userTv, bookmarked),
    pov.game.userIds.filter(isStreaming) map views.html.streamer.bits.contextual
  )

  def meta(
    pov: lidraughts.game.Pov,
    initialFen: Option[draughts.format.FEN],
    tour: Option[lidraughts.tournament.Tournament],
    simul: Option[lidraughts.simul.Simul],
    userTv: Option[lidraughts.user.User] = None,
    bookmarked: Boolean
  )(implicit ctx: Context): Option[Frag] = ctx.noBlind option {
    import pov._
    div(cls := "game__meta")(
      st.section(
        div(cls := "game__meta__infos", dataIcon := bits.gameIcon(game))(
          div(cls := "header")(
            div(cls := "setup")(
              views.html.bookmark.toggle(game, bookmarked),
              if (game.imported) frag(
                a(href := routes.Importer.importGame, title := trans.importGame.txt())("IMPORT"),
                separator,
                if (game.variant.exotic)
                  bits.variantLink(game.variant, game.variant.name.toUpperCase, initialFen = initialFen)
                else
                  game.variant.name.toUpperCase
              )
              else frag(
                widgets showClock game,
                separator,
                if (game.rated) trans.rated.txt() else trans.casual.txt(),
                separator,
                if (game.variant.exotic)
                  bits.variantLink(game.variant, game.variant.name.toUpperCase, initialFen = initialFen)
                else
                  game.perfType.map { pt =>
                    span(title := pt.title)(pt.shortName)
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
              (game.status != draughts.Status.Mate).option(separator),
              winner.color.fold(trans.whiteIsVictorious, trans.blackIsVictorious).frag()
            )
          }
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
          a(href := routes.Simul.show(sim.id))(sim.fullName),
          div(cls := "game__simul__infos")(
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

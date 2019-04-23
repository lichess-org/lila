package views.html
package game

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.game.{ Game, Pov, Player }

import controllers.routes

object widgets {

  private val separator = " • "

  def apply(
    games: Seq[Game],
    user: Option[lila.user.User] = None,
    ownerLink: Boolean = false,
    mini: Boolean = false
  )(implicit ctx: Context): Frag = games map { g =>
    val fromPlayer = user flatMap g.player
    val firstPlayer = fromPlayer | g.firstPlayer
    st.article(cls := "game-row paginated")(
      a(cls := "game-row__overlay", href := gameLink(g, firstPlayer.color, ownerLink)),
      div(cls := "game-row__board")(
        gameFen(Pov(g, firstPlayer), withLink = false, withTitle = false)
      ),
      div(cls := "game-row__infos")(
        div(cls := "header", dataIcon := bits.gameIcon(g))(
          div(cls := "header__text")(
            strong(
              if (g.imported) frag(
                span("IMPORT"),
                g.pgnImport.flatMap(_.user).map { user =>
                  frag(" ", trans.by(userIdLink(user.some, None, false)))
                },
                separator,
                if (g.variant.exotic) bits.variantLink(g.variant, g.variant.name.toUpperCase)
                else g.variant.name.toUpperCase
              )
              else frag(
                showClock(g),
                separator,
                g.perfType.fold(chess.variant.FromPosition.name)(_.name),
                separator,
                if (g.rated) trans.rated.txt() else trans.casual.txt()
              )
            ),
            g.pgnImport.flatMap(_.date).fold(momentFromNow(g.createdAt))(frag(_)),
            g.tournamentId map { tourId =>
              frag(separator, tournamentLink(tourId))
            },
            g.simulId map { simulId =>
              frag(separator, views.html.simul.bits.link(simulId))
            }
          )
        ),
        div(cls := "versus")(
          gamePlayer(g.variant, g.whitePlayer),
          div(cls := "swords", dataIcon := "U"),
          gamePlayer(g.variant, g.blackPlayer)
        ),
        div(cls := "result")(
          if (g.isBeingPlayed) trans.playingRightNow() else {
            if (g.finishedOrAborted)
              span(cls := g.winner.flatMap(w => fromPlayer.map(p => if (p == w) "win" else "loss")))(
                gameEndStatus(g),
                g.winner.map { winner =>
                  frag(
                    ", ",
                    winner.color.fold(trans.whiteIsVictorious(), trans.blackIsVictorious())
                  )
                }
              )
            else g.turnColor.fold(trans.whitePlays(), trans.blackPlays())
          }
        ),
        if (g.turns > 0) {
          val pgnMoves = g.pgnMoves take 20
          div(cls := "opening")(
            (!g.fromPosition ?? g.opening) map { opening =>
              strong(opening.opening.ecoName)
            },
            div(cls := "pgn")(
              pgnMoves.take(6).grouped(2).zipWithIndex map {
                case (Vector(w, b), i) => s"${i + 1}. $w $b"
                case (Vector(w), i) => s"${i + 1}. $w"
                case _ => ""
              } mkString " ",
              g.turns > 6 option s" ... ${1 + (g.turns - 1) / 2} moves "
            )
          )
        } else frag(br, br),
        g.metadata.analysed option
          div(cls := "metadata text", dataIcon := "")(trans.computerAnalysisAvailable()),
        g.pgnImport.flatMap(_.user).map { user =>
          div(cls := "metadata")("PGN import by ", userIdLink(user.some))
        }
      )
    )
  }

  def showClock(game: Game)(implicit ctx: Context) = game.clock.map { clock =>
    frag(clock.config.show)
  } getOrElse {
    game.daysPerTurn.map { days =>
      span(title := trans.correspondence.txt())(
        if (days == 1) trans.oneDay() else trans.nbDays.pluralSame(days)
      )
    }.getOrElse {
      span(title := trans.unlimited.txt())("∞")
    }
  }

  private lazy val anonSpan = span(cls := "anon")(lila.user.User.anonymous)

  private def gamePlayer(variant: chess.variant.Variant, player: Player)(implicit ctx: Context) =
    div(cls := s"player ${player.color.name}")(
      player.playerUser map { playerUser =>
        frag(
          userIdLink(playerUser.id.some, withOnline = false),
          br,
          player.berserk option berserkIconSpan,
          playerUser.rating,
          player.provisional option "?",
          playerUser.ratingDiff map { d => frag(" ", showRatingDiff(d)) }
        )
      } getOrElse {
        player.aiLevel map { level =>
          frag(
            span(aiName(level, false)),
            br,
            aiRating(level)
          )
        } getOrElse {
          (player.nameSplit.fold[Frag](anonSpan) {
            case (name, rating) => frag(
              span(name),
              rating.map { r => frag(br, r) }
            )
          })
        }
      }
    )
}

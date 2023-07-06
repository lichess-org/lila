package views.html
package game

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.game.{ Game, Player, Pov }

object widgets:

  private val separator = " • "

  def apply(
      games: Seq[Game],
      notes: Map[GameId, String] = Map(),
      user: Option[lila.user.User] = None,
      ownerLink: Boolean = false
  )(using Context): Frag =
    games map { g =>
      val fromPlayer  = user flatMap g.player
      val firstPlayer = fromPlayer | g.player(g.naturalOrientation)
      st.article(cls := "game-row paginated")(
        a(cls := "game-row__overlay", href := gameLink(g, firstPlayer.color, ownerLink)),
        div(cls := "game-row__board")(
          views.html.board.bits.mini(Pov(g, firstPlayer))(span)
        ),
        div(cls := "game-row__infos")(
          div(cls := "header", dataIcon := bits.gameIcon(g))(
            div(cls := "header__text")(
              strong(
                if g.imported then
                  frag(
                    span("IMPORT"),
                    g.pgnImport.flatMap(_.user).map { user =>
                      frag(" ", trans.by(userIdLink(user.some, None, withOnline = false)))
                    },
                    separator,
                    bits.variantLink(g.variant, g.perfType)
                  )
                else
                  frag(
                    showClock(g),
                    separator,
                    if g.fromPosition then g.variant.name else g.perfType.trans,
                    separator,
                    (if g.rated then trans.rated else trans.casual).txt()
                  )
              ),
              g.pgnImport.flatMap(_.date).fold[Frag](momentFromNowWithPreload(g.createdAt))(frag(_)),
              g.tournamentId.map { tourId =>
                frag(separator, tournamentLink(tourId))
              } orElse
                g.simulId.map { simulId =>
                  frag(separator, views.html.simul.bits.link(simulId))
                } orElse
                g.swissId.map { swissId =>
                  frag(separator, views.html.swiss.bits.link(SwissId(swissId)))
                }
            )
          ),
          div(cls := "versus")(
            gamePlayer(g.whitePlayer),
            div(cls := "swords", dataIcon := licon.Swords),
            gamePlayer(g.blackPlayer)
          ),
          div(cls := "result")(
            if g.isBeingPlayed then trans.playingRightNow()
            else if g.finishedOrAborted then
              span(cls := g.winner.flatMap(w => fromPlayer.map(p => if p == w then "win" else "loss")))(
                gameEndStatus(g),
                g.winner.map { winner =>
                  frag(
                    " • ",
                    winner.color.fold(trans.whiteIsVictorious(), trans.blackIsVictorious())
                  )
                }
              )
            else g.turnColor.fold(trans.whitePlays(), trans.blackPlays())
          ),
          if g.playedTurns > 0 then
            div(cls := "opening")(
              (!g.fromPosition so g.opening) map { opening =>
                strong(opening.opening.name)
              },
              div(cls := "pgn")(
                g.sans.take(6).grouped(2).zipWithIndex.map {
                  case (Vector(w, b), i) => s"${i + 1}. $w $b"
                  case (Vector(w), i)    => s"${i + 1}. $w"
                  case _                 => ""
                } mkString " ",
                g.ply > 6 option s" ... ${1 + (g.ply.value - 1) / 2} moves "
              )
            )
          else frag(br, br),
          notes get g.id map { note =>
            div(cls := "notes")(strong("Notes: "), note)
          },
          g.metadata.analysed option
            div(cls := "metadata text", dataIcon := licon.BarChart)(trans.computerAnalysisAvailable()),
          g.pgnImport.flatMap(_.user).map { user =>
            div(cls := "metadata")("PGN import by ", userIdLink(user.some))
          }
        )
      )
    }

  def showClock(game: Game)(using Context) =
    game.clock
      .map: clock =>
        frag(clock.config.show)
      .getOrElse:
        game.daysPerTurn
          .map: days =>
            span(title := trans.correspondence.txt()):
              if days.value == 1 then trans.oneDay()
              else trans.nbDays.pluralSame(days.value)
          .getOrElse:
            span(title := trans.unlimited.txt())("∞")

  private lazy val anonSpan = span(cls := "anon")(lila.user.User.anonymous)

  private def gamePlayer(player: Player)(using ctx: Context) =
    div(cls := s"player ${player.color.name}"):
      player.playerUser
        .map: playerUser =>
          frag(
            userIdLink(playerUser.id.some, withOnline = false),
            br,
            player.berserk option berserkIconSpan,
            ctx.pref.showRatings option frag(
              playerUser.rating,
              player.provisional.yes option "?",
              playerUser.ratingDiff map { d =>
                frag(" ", showRatingDiff(d))
              }
            )
          )
        .getOrElse:
          player.aiLevel
            .map: level =>
              span(aiNameFrag(level))
            .getOrElse:
              player.nameSplit.fold[Frag](anonSpan): (name, rating) =>
                frag(
                  span(name),
                  rating.map:
                    frag(br, _)
                )

package views.game

import lila.app.UiEnv.{ *, given }
import lila.core.game.Player
import lila.game.GameExt.perfType
import lila.game.Player.nameSplit
import lila.ui.Context

object widgets:

  private val separator = " • "

  def apply(
      games: Seq[Game],
      notes: Map[GameId, String] = Map(),
      user: Option[User] = None,
      ownerLink: Boolean = false
  )(using Context): Frag =
    games.map { g =>
      val fromPlayer  = user.flatMap(g.player)
      val firstPlayer = fromPlayer | g.player(g.naturalOrientation)
      st.article(cls := "game-row paginated")(
        a(cls := "game-row__overlay", href := gameLink(g, firstPlayer.color, ownerLink)),
        div(cls := "game-row__board")(
          views.board.mini(Pov(g, firstPlayer))(span)
        ),
        div(cls := "game-row__infos")(
          div(cls := "header", dataIcon := ui.gameIcon(g))(
            div(cls := "header__text")(
              strong(
                if g.sourceIs(_.Import) then
                  frag(
                    span("IMPORT"),
                    g.pgnImport.flatMap(_.user).map { user =>
                      frag(" ", trans.site.by(userIdLink(user.some, None, withOnline = false)))
                    },
                    separator,
                    variantLink(g.variant, g.perfType)
                  )
                else
                  frag(
                    showClock(g),
                    separator,
                    if g.fromPosition then g.variant.name else g.perfType.trans,
                    separator,
                    (if g.rated then trans.site.rated else trans.site.casual).txt()
                  )
              ),
              g.pgnImport.flatMap(_.date).fold[Frag](momentFromNowWithPreload(g.createdAt))(frag(_)),
              g.tournamentId
                .map { tourId =>
                  frag(separator, views.tournament.ui.tournamentLink(tourId))
                }
                .orElse(g.simulId.map { simulId =>
                  frag(separator, views.simul.ui.link(simulId))
                })
                .orElse(g.swissId.map { swissId =>
                  frag(separator, views.swiss.ui.link(swissId))
                })
            )
          ),
          div(cls := "versus")(
            gamePlayer(g.whitePlayer),
            div(cls := "swords", dataIcon := Icon.Swords),
            gamePlayer(g.blackPlayer)
          ),
          div(cls := "result")(
            if g.isBeingPlayed then trans.site.playingRightNow()
            else if g.finishedOrAborted then
              span(cls := g.winner.flatMap(w => fromPlayer.map(p => if p == w then "win" else "loss")))(
                ui.gameEndStatus(g),
                g.winner.map { winner =>
                  frag(
                    " • ",
                    winner.color.fold(trans.site.whiteIsVictorious(), trans.site.blackIsVictorious())
                  )
                }
              )
            else g.turnColor.fold(trans.site.whitePlays(), trans.site.blackPlays())
          ),
          if g.playedTurns > 0 then
            div(cls := "opening")(
              ((!g.fromPosition).so(g.opening)).map { opening =>
                strong(opening.opening.name)
              },
              div(cls := "pgn")(
                g.sans
                  .take(6)
                  .grouped(2)
                  .zipWithIndex
                  .map {
                    case (Vector(w, b), i) => s"${i + 1}. $w $b"
                    case (Vector(w), i)    => s"${i + 1}. $w"
                    case _                 => ""
                  }
                  .mkString(" "),
                (g.ply > 6).option(s" ... ${1 + (g.ply.value - 1) / 2} moves ")
              )
            )
          else frag(br, br),
          notes.get(g.id).map { note =>
            div(cls := "notes")(strong("Notes: "), note)
          },
          g.metadata.analysed.option(
            div(cls := "metadata text", dataIcon := Icon.BarChart)(trans.site.computerAnalysisAvailable())
          ),
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
            span(title := trans.site.correspondence.txt()):
              if days.value == 1 then trans.site.oneDay()
              else trans.site.nbDays.pluralSame(days.value)
          .getOrElse:
            span(title := trans.site.unlimited.txt())("∞")

  private lazy val anonSpan = span(cls := "anon")(UserName.anonymous)

  private def gamePlayer(player: Player)(using ctx: Context) =
    div(cls := s"player ${player.color.name}"):
      player.userId
        .flatMap: uid =>
          player.rating.map { (uid, _) }
        .map: (userId, rating) =>
          frag(
            userIdLink(userId.some, withOnline = false),
            br,
            player.berserk.option(berserkIconSpan),
            ctx.pref.showRatings.option(
              frag(
                rating,
                player.provisional.yes.option("?"),
                player.ratingDiff.map: d =>
                  frag(" ", showRatingDiff(d))
              )
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

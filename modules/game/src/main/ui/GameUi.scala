package lila.game
package ui

import chess.format.Fen
import chess.format.pgn.PgnStr

import lila.core.game.{ Game, Player }
import lila.game.GameExt.*
import lila.ui.*

import ScalatagsTemplate.{ *, given }
import lila.game.Player.nameSplit

final class GameUi(helpers: Helpers):
  import helpers.{ *, given }

  object mini:
    private val dataState = attr("data-state")
    private val dataLive = attr("data-live")
    private val dataTime = attr("data-time")
    private val dataTimeControl = attr("data-tc")
    val cgWrap = span(cls := "cg-wrap")(cgWrapContent)

    def apply(
        pov: Pov,
        ownerLink: Boolean = false,
        tv: Boolean = false,
        withLink: Boolean = true
    )(using
        ctx: Context
    ): Tag =
      renderMini(
        pov,
        withLink.option(gameLink(pov.game, pov.color, ownerLink, tv)),
        showRatings = ctx.pref.showRatings
      )

    def noCtx(pov: Pov, tv: Boolean = false, channelKey: Option[String] = None): Tag =
      val link = if tv then channelKey.fold(routes.Tv.index) { routes.Tv.onChannel }
      else routes.Round.watcher(pov.gameId, pov.color)
      renderMini(pov, link.url.some)(using transDefault, None)

    def renderState(pov: Pov)(using me: Option[Me]) =
      val fen =
        if me.flatMap(pov.game.player).exists(_.blindfold) && pov.game.playable
        then chess.format.BoardAndColorFen("8/8/8/8/8/8/8/8 w")
        else Fen.writeBoardAndColor(pov.game.position)
      dataState := s"${fen},${pov.color.name},${~pov.game.lastMoveKeys}"

    private def renderMini(
        pov: Pov,
        link: Option[String],
        showRatings: Boolean = true
    )(using Translate, Option[Me]): Tag =
      import pov.game
      val tag = if link.isDefined then a else span
      def showTimeControl(c: chess.Clock.Config) = s"${c.limitSeconds}+${c.increment}"
      tag(
        href := link,
        cls := s"mini-game mini-game-${game.id} mini-game--init ${game.variant.key} is2d",
        dataLive := game.isBeingPlayed.option(game.id),
        dataTimeControl := game.clock.map(_.config).fold("correspondence")(showTimeControl(_)),
        renderState(pov)
      )(
        renderPlayer(!pov, withRating = showRatings),
        cgWrap,
        renderPlayer(pov, withRating = showRatings)
      )

    private def renderPlayer(pov: Pov, withRating: Boolean)(using Translate) =
      span(cls := "mini-game__player")(
        span(cls := "mini-game__user")(
          playerUsername(pov.player.light, pov.player.userId.flatMap(lightUserSync), withRating = false),
          withRating.option(span(cls := "rating")(lila.game.Namer.ratingString(pov.player)))
        ),
        if pov.game.finished then renderResult(pov)
        else pov.game.clock.map { renderClock(_, pov.color) }
      )

    private def renderResult(pov: Pov) =
      span(cls := "mini-game__result"):
        pov.game.winnerColor.fold("½"): c =>
          if c == pov.color then "1" else "0"

    private def renderClock(clock: chess.Clock, color: Color) =
      val s = clock.remainingTime(color).roundSeconds.value
      span(
        cls := s"mini-game__clock mini-game__clock--${color.name}",
        dataTime := s
      ):
        f"${s / 60}:${s % 60}%02d"
  end mini

  def gameIcon(game: Game): Icon =
    if game.fromPosition then Icon.Feather
    else if game.sourceIs(_.Import) then Icon.UploadCloud
    else if game.variant.exotic then game.perfType.icon
    else if game.hasAi then Icon.Cogs
    else game.perfType.icon

  def gameEndStatus(game: Game)(using Translate): String =
    import chess.{ White, Black, Status as S }
    import lila.game.GameExt.drawReason
    game.status match
      case S.Aborted => trans.site.gameAborted.txt()
      case S.Mate => trans.site.checkmate.txt()
      case S.Resign =>
        (if game.loser.exists(_.color.white) then trans.site.whiteResigned else trans.site.blackResigned)
          .txt()
      case S.UnknownFinish => trans.site.finished.txt()
      case S.Stalemate => trans.site.stalemate.txt()
      case S.Timeout =>
        (game.loser, game.turnColor) match
          case (Some(p), _) if p.color.white => trans.site.whiteLeftTheGame.txt()
          case (Some(_), _) => trans.site.blackLeftTheGame.txt()
          case (None, White) => trans.site.whiteLeftTheGame.txt() + " • " + trans.site.draw.txt()
          case (None, Black) => trans.site.blackLeftTheGame.txt() + " • " + trans.site.draw.txt()
      case S.Draw =>
        import lila.game.DrawReason.*
        game.drawReason match
          case Some(MutualAgreement) => trans.site.drawByMutualAgreement.txt()
          case Some(FiftyMoves) => trans.site.fiftyMovesWithoutProgress.txt() + " • " + trans.site.draw.txt()
          case Some(ThreefoldRepetition) =>
            trans.site.threefoldRepetition.txt() + " • " + trans.site.draw.txt()
          case Some(InsufficientMaterial) =>
            trans.site.insufficientMaterial.txt() + " • " + trans.site.draw.txt()
          case _ => trans.site.draw.txt()
      case S.InsufficientMaterialClaim =>
        trans.site.drawClaimed.txt() + " • " + trans.site.insufficientMaterial.txt()
      case S.Outoftime =>
        (game.turnColor, game.loser) match
          case (White, Some(_)) => trans.site.whiteTimeOut.txt()
          case (White, None) => trans.site.whiteTimeOut.txt() + " • " + trans.site.draw.txt()
          case (Black, Some(_)) => trans.site.blackTimeOut.txt()
          case (Black, None) => trans.site.blackTimeOut.txt() + " • " + trans.site.draw.txt()
      case S.NoStart =>
        (if game.loser.exists(_.color.white) then trans.site.whiteDidntMove else trans.site.blackDidntMove)
          .txt()
      case S.Cheat => trans.site.cheatDetected.txt()
      case S.VariantEnd =>
        game.variant match
          case chess.variant.KingOfTheHill => trans.site.kingInTheCenter.txt()
          case chess.variant.ThreeCheck => trans.site.threeChecks.txt()
          case chess.variant.RacingKings => trans.site.raceFinished.txt()
          case _ => trans.site.variantEnding.txt()
      case _ => ""

  object crosstable:

    def option(cross: Option[lila.game.Crosstable.WithMatchup], game: Game)(using ctx: Context) =
      cross.map: c =>
        apply(ctx.userId.fold(c)(c.fromPov), game.id.some)

    def apply(ct: Crosstable.WithMatchup, currentId: Option[GameId])(using Context): Tag =
      apply(ct.crosstable, ct.matchup, currentId)

    def apply(ct: Crosstable, trueMatchup: Option[Crosstable.Matchup], currentId: Option[GameId])(using
        Context
    ): Tag =
      val matchup = trueMatchup.filter(_.users != ct.users)
      val matchupSepAt: Option[Int] = matchup.map: m =>
        (ct.nbGames.min(Crosstable.maxGames)) - m.users.nbGames

      div(cls := "crosstable")(
        (ct.fillSize > 0).option(raw(s"""<fill style="flex:${ct.fillSize * 0.75} 1 auto"></fill>""")),
        ct.results.mapWithIndex: (r, i) =>
          tag("povs")(
            cls := List(
              "sep" -> matchupSepAt.has(i),
              "current" -> currentId.has(r.gameId)
            )
          ):
            ct.users.toList.map: u =>
              val (linkClass, text) = r.winnerId match
                case Some(w) if w == u.id => "glpt win" -> "1"
                case None => "glpt" -> "½"
                case _ => "glpt loss" -> "0"
              a(href := s"""${routes.Round.watcher(r.gameId, Color.white)}?pov=${u.id}""", cls := linkClass)(
                text
              )
        ,
        matchup.map: m =>
          div(cls := "crosstable__matchup force-ltr", title := trans.site.currentMatchScore.txt()):
            ct.users.toList.map: u =>
              span(cls := m.users.winnerId.map(w => if w == u.id then "win" else "loss"))(
                m.users.showScore(u.id)
              )
        ,
        div(cls := "crosstable__users"):
          ct.users.toList.map: u =>
            userIdLink(u.id.some, withOnline = false)
        ,
        div(cls := "crosstable__score force-ltr", title := trans.site.lifetimeScore.txt()):
          ct.users.toList.map: u =>
            span(cls := ct.users.winnerId.map(w => if w == u.id then "win" else "loss"))(ct.showScore(u.id))
      )

  object importer:

    private def analyseHelp(using ctx: Context) =
      (!ctx.isAuth).option:
        a(cls := "blue", href := routes.Auth.signup)(trans.site.youNeedAnAccountToDoThat())

    def apply(form: play.api.data.Form[?])(using ctx: Context) =
      Page(trans.site.importGame.txt())
        .css("bits.importer")
        .js(esmInitBit("importer"))
        .graph(
          title = "Paste PGN chess game",
          url = routeUrl(routes.Importer.importGame),
          description = trans.site.importGameExplanation.txt()
        ):
          main(cls := "importer page-small box box-pad")(
            h1(cls := "box__top")(trans.site.importGame()),
            p(cls := "explanation")(
              trans.site.importGameExplanation(),
              br,
              a(cls := "text", dataIcon := Icon.InfoCircle, href := routes.Study.allDefault(1)):
                trans.site.importGameDataPrivacyWarning()
            ),
            standardFlash,
            postForm(cls := "form3 import", action := routes.Importer.sendGame)(
              form3.group(form("pgn"), trans.site.pasteThePgnStringHere())(form3.textarea(_)()),
              form("pgn").value.flatMap { pgn =>
                lila.game.importer
                  .parseImport(PgnStr(pgn), ctx.userId)
                  .fold(
                    err => frag(pre(cls := "error")(err), br, br).some,
                    _ => none
                  )
              },
              form3.group(form("pgnFile"), trans.site.orUploadPgnFile(), klass = "upload"): f =>
                form3.file.pgn(f.name),
              form3.nativeCheckboxField(
                form("analyse"),
                trans.site.requestAComputerAnalysis(),
                help = analyseHelp,
                half = false,
                value = "1",
                klass = "analyse"
              ),
              form3.action(form3.submit(trans.site.importGame(), Icon.UploadCloud.some))
            )
          )

  object widgets:

    val separator = " • "

    def apply(g: Game, note: Option[String], user: Option[User], ownerLink: Boolean)(
        contextLink: Option[Tag]
    )(using Context): Frag =
      val fromPlayer = user.flatMap(g.player)
      val firstPlayer = fromPlayer | g.player(g.naturalOrientation)
      st.article(cls := "game-row paginated")(
        a(cls := "game-row__overlay", href := gameLink(g, firstPlayer.color, ownerLink)),
        div(cls := "game-row__board")(miniBoard(Pov(g, firstPlayer))(span)),
        div(cls := "game-row__infos")(
          div(cls := "header", dataIcon := gameIcon(g))(
            div(cls := "header__text")(
              source(g),
              g.pgnImport.flatMap(_.date).fold[Frag](momentFromNowWithPreload(g.createdAt))(frag(_)),
              contextLink.map(l => frag(separator, l))
            )
          ),
          content(g, note, fromPlayer)
        )
      )

    def miniBoard(pov: Pov)(using ctx: Context): Tag => Tag =
      chessgroundMini(
        if ctx.me.flatMap(pov.game.player).exists(_.blindfold) && pov.game.playable
        then Fen.Board("8/8/8/8/8/8/8/8")
        else Fen.writeBoard(pov.game.position),
        if pov.game.variant == chess.variant.RacingKings then chess.White else pov.player.color,
        pov.game.history.lastMove
      )

    def content(g: Game, note: Option[String], as: Option[Player])(using Context) = frag(
      div(cls := "versus")(
        gamePlayer(g.whitePlayer),
        div(cls := "swords", dataIcon := Icon.Swords),
        gamePlayer(g.blackPlayer)
      ),
      result(g, as),
      if g.playedPlies > 0 then opening(g) else frag(br, br),
      note.map: note =>
        div(cls := "notes")(strong("Notes: "), note),
      g.metadata.analysed.option(
        div(cls := "metadata text", dataIcon := Icon.BarChart)(trans.site.computerAnalysisAvailable())
      ),
      g.pgnImport.flatMap(_.user).map { user =>
        div(cls := "metadata")("PGN import by ", userIdLink(user.some))
      }
    )

    def source(g: Game)(using Context) =
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
            ratedName(g.rated)
          )
      )

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
                player.nameSplit.fold(span(cls := "anon")(UserName.anonymous)): (name, rating) =>
                  frag(
                    span(name),
                    rating.map:
                      frag(br, _)
                  )

    private def opening(g: Game) =
      div(cls := "opening")(
        g.fromPosition.not.so(g.opening).map { opening =>
          strong(opening.opening.name)
        },
        div(cls := "pgn")(
          g.sans
            .take(6)
            .grouped(2)
            .zipWithIndex
            .map:
              case (Vector(w, b), i) => s"${i + 1}. $w $b"
              case (Vector(w), i) => s"${i + 1}. $w"
              case _ => ""
            .mkString(" "),
          (g.ply > 6).option(s" ... ${1 + (g.ply.value - 1) / 2} moves ")
        )
      )

    private def result(g: Game, as: Option[Player])(using Context) = div(cls := "result")(
      if g.isBeingPlayed then trans.site.playingRightNow()
      else if g.finishedOrAborted then
        span(cls := g.winner.flatMap(w => as.map(p => if p == w then "win" else "loss")))(
          gameEndStatus(g),
          g.winner.map: winner =>
            frag(
              " • ",
              winner.color.fold(trans.site.whiteIsVictorious(), trans.site.blackIsVictorious())
            )
        )
      else g.turnColor.fold(trans.site.whitePlays(), trans.site.blackPlays())
    )

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

package lila.game
package ui

import chess.format.Fen
import chess.format.pgn.PgnStr

import lila.core.game.Game
import lila.game.GameExt.*
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class GameUi(helpers: Helpers):
  import helpers.{ *, given }

  object mini:
    private val dataState       = attr("data-state")
    private val dataLive        = attr("data-live")
    private val dataTime        = attr("data-time")
    private val dataTimeControl = attr("data-tc")
    val cgWrap                  = span(cls := "cg-wrap")(cgWrapContent)

    def apply(pov: Pov, ownerLink: Boolean = false, tv: Boolean = false, withLink: Boolean = true)(using
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
        else Fen.writeBoardAndColor(pov.game.situation)
      dataState := s"${fen},${pov.color.name},${~pov.game.lastMoveKeys}"

    private def renderMini(
        pov: Pov,
        link: Option[String] = None,
        showRatings: Boolean = true
    )(using Translate, Option[Me]): Tag =
      import pov.game
      val tag                                    = if link.isDefined then a else span
      def showTimeControl(c: chess.Clock.Config) = s"${c.limitSeconds}+${c.increment}"
      tag(
        href            := link,
        cls             := s"mini-game mini-game-${game.id} mini-game--init ${game.variant.key} is2d",
        dataLive        := game.isBeingPlayed.option(game.id),
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
      val s = clock.remainingTime(color).roundSeconds
      span(
        cls      := s"mini-game__clock mini-game__clock--${color.name}",
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
      case S.Mate    => trans.site.checkmate.txt()
      case S.Resign =>
        (if game.loser.exists(_.color.white) then trans.site.whiteResigned else trans.site.blackResigned)
          .txt()
      case S.UnknownFinish => trans.site.finished.txt()
      case S.Stalemate     => trans.site.stalemate.txt()
      case S.Timeout =>
        (game.loser, game.turnColor) match
          case (Some(p), _) if p.color.white => trans.site.whiteLeftTheGame.txt()
          case (Some(_), _)                  => trans.site.blackLeftTheGame.txt()
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
      case S.Outoftime =>
        (game.turnColor, game.loser) match
          case (White, Some(_)) => trans.site.whiteTimeOut.txt()
          case (White, None)    => trans.site.whiteTimeOut.txt() + " • " + trans.site.draw.txt()
          case (Black, Some(_)) => trans.site.blackTimeOut.txt()
          case (Black, None)    => trans.site.blackTimeOut.txt() + " • " + trans.site.draw.txt()
      case S.NoStart =>
        (if game.loser.exists(_.color.white) then trans.site.whiteDidntMove else trans.site.blackDidntMove)
          .txt()
      case S.Cheat => trans.site.cheatDetected.txt()
      case S.VariantEnd =>
        game.variant match
          case chess.variant.KingOfTheHill => trans.site.kingInTheCenter.txt()
          case chess.variant.ThreeCheck    => trans.site.threeChecks.txt()
          case chess.variant.RacingKings   => trans.site.raceFinished.txt()
          case _                           => trans.site.variantEnding.txt()
      case _ => ""

  object crosstable:

    def apply(ct: Crosstable.WithMatchup, currentId: Option[GameId])(using Context): Frag =
      apply(ct.crosstable, ct.matchup, currentId)

    def apply(ct: Crosstable, trueMatchup: Option[Crosstable.Matchup], currentId: Option[GameId])(using
        Context
    ): Frag =
      val matchup = trueMatchup.filter(_.users != ct.users)
      val matchupSepAt: Option[Int] = matchup.map: m =>
        (ct.nbGames.min(Crosstable.maxGames)) - m.users.nbGames

      div(cls := "crosstable")(
        (ct.fillSize > 0).option(raw(s"""<fill style="flex:${ct.fillSize * 0.75} 1 auto"></fill>""")),
        ct.results.mapWithIndex: (r, i) =>
          tag("povs")(
            cls := List(
              "sep"     -> matchupSepAt.has(i),
              "current" -> currentId.has(r.gameId)
            )
          ):
            ct.users.toList.map: u =>
              val (linkClass, text) = r.winnerId match
                case Some(w) if w == u.id => "glpt win"  -> "1"
                case None                 => "glpt"      -> "½"
                case _                    => "glpt loss" -> "0"
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
        .js(EsmInit("bits.importer"))
        .graph(
          title = "Paste PGN chess game",
          url = s"$netBaseUrl${routes.Importer.importGame.url}",
          description = trans.site.importGameExplanation.txt()
        ):
          main(cls := "importer page-small box box-pad")(
            h1(cls := "box__top")(trans.site.importGame()),
            p(cls := "explanation")(
              trans.site.importGameExplanation(),
              br,
              a(cls := "text", dataIcon := Icon.InfoCircle, href := routes.Study.allDefault()):
                trans.site.importGameCaveat()
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
              form3.checkbox(
                form("analyse"),
                trans.site.requestAComputerAnalysis(),
                help = Some(analyseHelp),
                disabled = !ctx.isAuth
              ),
              a(cls := "text", dataIcon := Icon.InfoCircle, href := routes.Study.allDefault(1)):
                trans.site.importGameDataPrivacyWarning()
              ,
              form3.action(form3.submit(trans.site.importGame(), Icon.UploadCloud.some))
            )
          )

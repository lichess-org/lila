package lila.game
package ui

import lila.ui.*
import ScalatagsTemplate.{ *, given }
import lila.core.game.Game
import lila.game.GameExt.*

final class GameUi(helpers: Helpers):
  import helpers.{ *, given }

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
              a(href := s"""${routes.Round.watcher(r.gameId, "white")}?pov=${u.id}""", cls := linkClass)(
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

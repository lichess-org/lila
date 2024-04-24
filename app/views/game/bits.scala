package views.html.game

import play.api.i18n.Lang

import lila.app.templating.Environment.{ *, given }
import lila.ui.ScalatagsTemplate.*

import lila.rating.PerfType
import lila.game.GameExt.perfType

lazy val ui =
  lila.game.ui.GameUi(i18nHelper, dateHelper, userHelper)(routeRoundWatcher = routes.Round.watcher)

object bits:

  def sides(
      pov: Pov,
      initialFen: Option[chess.format.Fen.Full],
      tour: Option[lila.tournament.TourAndTeamVs],
      cross: Option[lila.game.Crosstable.WithMatchup],
      simul: Option[lila.simul.Simul],
      userTv: Option[User] = None,
      bookmarked: Boolean
  )(using ctx: Context) =
    div(
      side.meta(pov, initialFen, tour, simul, userTv, bookmarked = bookmarked),
      cross.map: c =>
        div(cls := "crosstable")(ui.crosstable(ctx.userId.fold(c)(c.fromPov), pov.gameId.some))
    )

  def variantLink(
      variant: chess.variant.Variant,
      perfType: PerfType,
      initialFen: Option[chess.format.Fen.Full] = None,
      shortName: Boolean = false
  )(using Translate): Frag =

    def link(href: String, title: String, name: String) = a(
      cls     := "variant-link",
      st.href := href,
      targetBlank,
      st.title := title
    )(name)

    if variant.exotic then
      link(
        href = variant match
          case chess.variant.FromPosition =>
            s"""${routes.Editor.index}?fen=${initialFen.so(_.value.replace(' ', '_'))}"""
          case v => routes.Cms.variant(v.key).url
        ,
        title = variant.title,
        name = (if shortName && variant == chess.variant.KingOfTheHill then variant.shortName
                else variant.name).toUpperCase
      )
    else if perfType == PerfType.Correspondence then
      link(
        href = s"${routes.Main.faq}#correspondence",
        title = PerfType.Correspondence.desc,
        name = PerfType.Correspondence.trans
      )
    else span(title := perfType.desc)(perfType.trans)

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

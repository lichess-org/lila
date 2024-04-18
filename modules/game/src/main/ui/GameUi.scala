package lila.game
package ui

import lila.ui.ScalatagsTemplate.{ *, given }
import lila.ui.Context
import lila.core.game.Game
import lila.ui.Icon
import lila.game.GameExt.*

final class GameUi(i18nHelper: lila.ui.I18nHelper, dateHelper: lila.ui.DateHelper)(
    routeRoundWatcher: (String, String) => Call
):
  import i18nHelper.{ *, given }

  def gameIcon(game: Game): Icon =
    if game.fromPosition then Icon.Feather
    else if game.sourceIs(_.Import) then Icon.UploadCloud
    else if game.variant.exotic then game.perfType.icon
    else if game.hasAi then Icon.Cogs
    else game.perfType.icon

  final class crosstable(userLink: UserId => Translate ?=> Frag):

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
              a(href := s"""${routeRoundWatcher(r.gameId, "white")}?pov=${u.id}""", cls := linkClass)(
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
            userLink(u.id)
        ,
        div(cls := "crosstable__score force-ltr", title := trans.site.lifetimeScore.txt()):
          ct.users.toList.map: u =>
            span(cls := ct.users.winnerId.map(w => if w == u.id then "win" else "loss"))(ct.showScore(u.id))
      )

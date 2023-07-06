package views.html.game

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.game.Crosstable

import controllers.routes

object crosstable:

  def apply(ct: Crosstable.WithMatchup, currentId: Option[GameId])(using Context): Frag =
    apply(ct.crosstable, ct.matchup, currentId)

  def apply(ct: Crosstable, trueMatchup: Option[Crosstable.Matchup], currentId: Option[GameId])(using
      Context
  ): Frag =
    val matchup = trueMatchup.filter(_.users != ct.users)
    val matchupSepAt: Option[Int] = matchup.map: m =>
      (ct.nbGames min Crosstable.maxGames) - m.users.nbGames

    div(cls := "crosstable")(
      ct.fillSize > 0 option raw(s"""<fill style="flex:${ct.fillSize * 0.75} 1 auto"></fill>"""),
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
            a(href := s"""${routes.Round.watcher(r.gameId, "white")}?pov=${u.id}""", cls := linkClass)(text)
      ,
      matchup.map: m =>
        div(cls := "crosstable__matchup force-ltr", title := trans.currentMatchScore.txt()):
          ct.users.toList.map: u =>
            span(cls := m.users.winnerId.map(w => if w == u.id then "win" else "loss"))(
              m.users.showScore(u.id)
            )
      ,
      div(cls := "crosstable__users"):
        ct.users.toList.map: u =>
          userIdLink(u.id.some, withOnline = false)
      ,
      div(cls := "crosstable__score force-ltr", title := trans.lifetimeScore.txt()):
        ct.users.toList.map: u =>
          span(cls := ct.users.winnerId.map(w => if w == u.id then "win" else "loss"))(ct.showScore(u.id))
    )

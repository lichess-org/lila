package views.html.lobby

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.*
import lila.game.Pov

object blindLobby:

  def apply(games: List[Pov])(using PageContext) =
    div(
      h2(games.size, " ongoing games"),
      games.nonEmpty option ongoingGames(games),
      div(cls := "lobby__app")
    )

  private def ongoingGames(games: List[Pov])(using PageContext) =
    games.partition(_.isMyTurn) match
      case (myTurn, opTurn) =>
        frag(
          h3("My turn: ", myTurn.size, " games"),
          ul(myTurn map renderGame),
          h3("Opponent turn: ", opTurn.size, " games"),
          ul(opTurn map renderGame)
        )

  private def renderGame(pov: Pov)(using PageContext) =
    li(
      a(href := gameLink(pov))(
        playerText(pov.opponent),
        " ",
        pov.isMyTurn so pov.remainingSeconds map { secondsFromNow(_, alwaysRelative = true) }
      )
    )

package views.html.lobby

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.game.Pov

object blindLobby {

  def apply(games: List[Pov])(implicit ctx: Context) =
    div(
      h2(games.size, " ongoing games"),
      games.nonEmpty option ongoingGames(games),
      div(cls := "lobby__app"),
    )

  private def ongoingGames(games: List[Pov])(implicit ctx: Context) =
    games.partition(_.isMyTurn) match {
      case (myTurn, opTurn) =>
        frag(
          h3(trans.yourTurn.txt(), ":", trans.nbGames.pluralSame(myTurn.size)),
          ul(myTurn map renderGame),
          h3(trans.waitingForOpponent.txt(), ":", trans.nbGames.pluralSame(opTurn.size)),
          ul(opTurn map renderGame),
        )
    }

  private def renderGame(pov: Pov)(implicit ctx: Context) =
    li(
      a(href := gameLink(pov))(
        playerText(pov.opponent),
        " ",
        pov.isMyTurn ?? pov.remainingSeconds map { secondsFromNow(_, true) },
      ),
    )
}

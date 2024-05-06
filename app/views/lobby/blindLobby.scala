package views.lobby

import lila.app.UiEnv.{ *, given }

object blindLobby:

  def apply(games: List[Pov])(using Context) =
    div(
      h2(games.size, " ongoing games"),
      games.nonEmpty.option(ongoingGames(games)),
      div(cls := "lobby__app")
    )

  private def ongoingGames(games: List[Pov])(using Context) =
    games.partition(_.isMyTurn) match
      case (myTurn, opTurn) =>
        frag(
          h3("My turn: ", myTurn.size, " games"),
          ul(myTurn.map(renderGame)),
          h3("Opponent turn: ", opTurn.size, " games"),
          ul(opTurn.map(renderGame))
        )

  private def renderGame(pov: Pov)(using Context) =
    li(
      a(href := gameLink(pov))(
        playerText(pov.opponent),
        " ",
        pov.isMyTurn.so(pov.remainingSeconds).map { secondsFromNow(_, alwaysRelative = true) }
      )
    )

package views.lobby

import lila.app.UiEnv.{ *, given }

object blindLobby:

  def apply(games: List[Pov])(using Context) =
    div(
      h2(trans.swiss.ongoingGames(games.size)),
      games.nonEmpty.option(ongoingGames(games)),
      div(cls := "lobby__app")
    )

  private def ongoingGames(games: List[Pov])(using Context) =
    val (myTurn, opTurn) = games.partition(_.isMyTurn)
    frag(
      h3(trans.site.yourTurn(), " : ", trans.site.nbGames.plural(myTurn.size, myTurn.size.localize)),
      ul(myTurn.map(renderGame)),
      h3(
        trans.site.waitingForOpponent(),
        " : ",
        trans.site.nbGames.plural(opTurn.size, opTurn.size.localize)
      ),
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

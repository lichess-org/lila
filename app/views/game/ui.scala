package views.game

import lila.app.UiEnv.*

val ui = lila.game.ui.GameUi(helpers)
export ui.mini

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

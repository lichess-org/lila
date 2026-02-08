package views.game

import lila.app.UiEnv.*
import lila.bookmark.Bookmark

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
      div(cls := "crosstable")(ui.crosstable(ctx.userId.foldLeft(c)(_.fromPov(_)), pov.gameId.some))
  )

def widgets(
    games: Seq[Game],
    notes: Map[GameId, String] = Map(),
    user: Option[User] = None,
    ownerLink: Boolean = false,
    bookmarks: Map[GameId, Bookmark] = Map()
)(using ctx: lila.ui.Context): Frag =
  games.map: g =>
    ui.widgets(g, notes.get(g.id), user, ownerLink, bookmarks.get(g.id).flatMap(_.position)):
      g.tournamentId
        .map: tourId =>
          views.tournament.ui.tournamentLink(tourId)(using ctx.translate)
        .orElse(g.simulId.map: simulId =>
          views.simul.ui.link(simulId))
        .orElse(g.swissId.map: swissId =>
          views.swiss.ui.link(swissId))

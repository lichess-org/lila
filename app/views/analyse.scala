package views.analyse

import chess.format.pgn.PgnStr

import lila.app.UiEnv.{ *, given }
import lila.round.RoundGame.secondsSinceCreation

val ui = lila.analyse.ui.AnalyseUi(helpers)(analyseEndpoints)

object replay:

  private val replayUi = lila.analyse.ui.ReplayUi(helpers)(ui)

  def forCrawler(
      pov: Pov,
      initialFen: Option[chess.format.Fen.Full],
      pgn: PgnStr,
      simul: Option[lila.simul.Simul],
      cross: Option[lila.game.Crosstable.WithMatchup]
  )(using Context) =
    replayUi.forCrawler(
      pov,
      pgn,
      graph = views.round.ui.povOpenGraph(pov),
      gameSide = views.game.side(pov, initialFen, none, simul = simul, bookmarked = false),
      crosstable = cross.map: c =>
        views.game.ui.crosstable(pov.player.userId.fold(c)(c.fromPov), pov.gameId.some)
    )

  def forBrowser(
      pov: Pov,
      data: play.api.libs.json.JsObject,
      initialFen: Option[chess.format.Fen.Full],
      pgn: PgnStr,
      analysis: Option[lila.analyse.Analysis],
      analysisStarted: Boolean,
      simul: Option[lila.simul.Simul],
      cross: Option[lila.game.Crosstable.WithMatchup],
      userTv: Option[User],
      chatOption: Option[lila.chat.UserChat.Mine],
      bookmarked: Boolean
  )(using ctx: Context) =

    val chatOpt = chatOption.map: c =>
      views.chat.json(
        c.chat,
        c.lines,
        name = trans.site.spectatorRoom.txt(),
        timeout = c.timeout,
        withNoteAge = ctx.isAuth.option(pov.game.secondsSinceCreation),
        public = true,
        resourceId = lila.chat.Chat.ResourceId(s"game/${c.chat.id}"),
        palantir = ctx.canPalantir
      ) -> views.chat.frag

    val side = views.game.side(pov, initialFen, none, simul = simul, userTv = userTv, bookmarked = bookmarked)

    replayUi.forBrowser(
      pov,
      data,
      pgn,
      analysable = lila.game.GameExt.analysable(pov.game),
      hasAnalysis = analysis.isDefined || analysisStarted,
      graph = views.round.ui.povOpenGraph(pov),
      gameSide = side,
      crosstable = cross.map: c =>
        views.game.ui.crosstable(pov.player.userId.fold(c)(c.fromPov), pov.gameId.some),
      chatOption = chatOpt
    )

object embed:

  def lpv(pgn: PgnStr, orientation: Option[Color], getPgn: Boolean)(using ctx: EmbedContext) =
    views.base.embed.minimal(
      title = "Lichess PGN viewer",
      cssKeys = List("bits.lpv.embed"),
      modules = Esm("site.lpvEmbed")
    )(
      div(cls := "is2d")(div(pgn)),
      ui.embed.lpvJs(orientation, getPgn)(ctx.nonce.some)
    )

  def notFound(using EmbedContext) =
    views.base.embed.minimal(
      title = "404 - Game not found",
      cssKeys = List("bits.lpv.embed")
    ):
      div(cls := "not-found")(h1("Game not found"))

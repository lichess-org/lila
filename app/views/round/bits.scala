package views.html
package round

import chess.variant.{ Crazyhouse, Variant }

import scala.util.chaining.*

import lila.app.templating.Environment.{ *, given }
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.common.Json.given
import lila.web.LangPath
import lila.game.GameExt.playerBlurPercent

object bits:

  def povOpenGraph(pov: Pov) =
    lila.web.OpenGraph(
      image = cdnUrl(routes.Export.gameThumbnail(pov.gameId, None, None).url).some,
      title = titleGame(pov.game),
      url = s"$netBaseUrl${routes.Round.watcher(pov.gameId, pov.color.name).url}",
      description = describePov(pov)
    )

  // #TODO RoundUi
  def describePov(pov: Pov) =
    import pov.*
    val p1    = playerText(game.whitePlayer, withRating = true)
    val p2    = playerText(game.blackPlayer, withRating = true)
    val plays = if game.finishedOrAborted then "played" else "is playing"
    val speedAndClock =
      if game.sourceIs(_.Import) then "imported"
      else
        game.clock.fold(chess.Speed.Correspondence.name): c =>
          s"${chess.Speed(c.config).name} (${c.config.show})"

    val mode = game.mode.name
    val variant =
      if game.variant == chess.variant.FromPosition
      then "position setup chess"
      else if game.variant.exotic
      then game.variant.name
      else "chess"
    import chess.Status.*
    val result = (game.winner, game.loser, game.status) match
      case (Some(w), _, Mate)                               => s"${playerText(w)} won by checkmate"
      case (_, Some(l), Resign | Timeout | Cheat | NoStart) => s"${playerText(l)} resigned"
      case (_, Some(l), Outoftime)                          => s"${playerText(l)} ran out of time"
      case (Some(w), _, UnknownFinish | VariantEnd)         => s"${playerText(w)} won"
      case (_, _, Draw | Stalemate | UnknownFinish)         => "Game is a draw"
      case (_, _, Aborted)                                  => "Game has been aborted"
      case _ if game.finished                               => "Game ended"
      case _                                                => "Game is still ongoing"
    val moves = (game.ply.value - game.startedAtPly.value + 1) / 2
    s"$p1 $plays $p2 in a $mode $speedAndClock game of $variant. $result after ${pluralize("move", moves)}. Click to replay, analyse, and discuss the game!"

  def layout(
      variant: Variant,
      title: String,
      pageModule: Option[PageModule],
      moreJs: Frag = emptyFrag,
      modules: EsmList = Nil,
      openGraph: Option[lila.web.OpenGraph] = None,
      moreCss: Frag = emptyFrag,
      playing: Boolean = false,
      zenable: Boolean = false,
      robots: Boolean = false,
      withHrefLangs: Option[LangPath] = None
  )(body: Frag)(using ctx: PageContext) =
    views.html.base.layout(
      title = title,
      openGraph = openGraph,
      moreJs = moreJs,
      moreCss = frag(
        cssTag(if variant == Crazyhouse then "round.zh" else "round"),
        ctx.pref.hasKeyboardMove.option(cssTag("keyboardMove")),
        ctx.pref.hasVoice.option(cssTag("voice")),
        ctx.blind.option(cssTag("round.nvui")),
        moreCss
      ),
      modules = modules,
      pageModule = pageModule,
      playing = playing,
      zenable = zenable,
      robots = robots,
      zoomable = true,
      csp = defaultCsp.withPeer.withWebAssembly.some,
      withHrefLangs = withHrefLangs
    )(body)

  def crosstable(cross: Option[lila.game.Crosstable.WithMatchup], game: Game)(using ctx: Context) =
    cross.map: c =>
      views.html.game.ui.crosstable(ctx.userId.fold(c)(c.fromPov), game.id.some)

  def underchat(game: Game)(using ctx: Context) =
    frag(
      views.html.chat.spectatorsFrag,
      isGranted(_.ViewBlurs).option(
        div(cls := "round__mod")(
          game.players.all
            .filter(p => game.playerBlurPercent(p.color) > 30)
            .map { p =>
              div(
                playerLink(
                  p,
                  cssClass = s"is color-icon ${p.color.name}".some,
                  withOnline = false,
                  mod = true
                ),
                s" ${p.blurs.nb}/${game.playerMoves(p.color)} blurs ",
                strong(game.playerBlurPercent(p.color), "%")
              )
            }
            // game.players flatMap { p => p.holdAlert.map(p ->) } map {
            //   case (p, h) => div(
            //     playerLink(p, cssClass = s"is color-icon ${p.color.name}".some, mod = true, withOnline = false),
            //     "hold alert",
            //     br,
            //     s"(ply: ${h.ply}, mean: ${h.mean} ms, SD: ${h.sd})"
            //   )
            // }
        )
      )
    )

  def others(playing: List[Pov], simul: Option[lila.simul.Simul])(using Context) =
    frag(
      h3(
        simul.fold(trans.site.currentGames()): s =>
          span(cls := "simul")(
            a(href := routes.Simul.show(s.id))("SIMUL"),
            span(cls := "win")(s.wins, " W"),
            " / ",
            span(cls := "draw")(s.draws, " D"),
            " / ",
            span(cls := "loss")(s.losses, " L"),
            " / ",
            s.ongoing,
            " ongoing"
          ),
        "round-toggle-autoswitch".pipe: id =>
          span(
            cls      := "move-on switcher",
            st.title := trans.site.automaticallyProceedToNextGameAfterMoving.txt()
          )(
            label(`for` := id)(trans.site.autoSwitch()),
            span(cls := "switch")(form3.cmnToggle(id, id, checked = false))
          )
      ),
      div(cls := "now-playing"):
        val (myTurn, otherTurn) = playing.partition(_.isMyTurn)
        (myTurn ++ otherTurn.take(6 - myTurn.size))
          .take(9)
          .map: pov =>
            a(href := routes.Round.player(pov.fullId), cls := pov.isMyTurn.option("my_turn"))(
              span(
                cls := s"mini-game mini-game--init ${pov.game.variant.key} is2d",
                views.html.game.mini.renderState(pov)
              )(views.html.game.mini.cgWrap),
              span(cls := "meta")(
                playerUsername(
                  pov.opponent.light,
                  pov.opponent.userId.flatMap(lightUserSync),
                  withRating = false,
                  withTitle = true
                ),
                span(cls := "indicator")(
                  if pov.isMyTurn then
                    pov.remainingSeconds
                      .fold[Frag](trans.site.yourTurn())(secondsFromNow(_, alwaysRelative = true))
                  else nbsp
                )
              )
            )
    )

  private[round] def side(
      pov: Pov,
      data: play.api.libs.json.JsObject,
      tour: Option[lila.tournament.TourAndTeamVs],
      simul: Option[lila.simul.Simul],
      userTv: Option[User] = None,
      bookmarked: Boolean
  )(using Context) =
    views.html.game.side(
      pov,
      (data \ "game" \ "initialFen").asOpt[chess.format.Fen.Full],
      tour,
      simul = simul,
      userTv = userTv,
      bookmarked = bookmarked
    )

  private[round] def povChessground(pov: Pov)(using ctx: Context): Frag =
    chessground(
      board = pov.game.board,
      orient = pov.color,
      lastMove = pov.game.history.lastMove
        .map(_.origDest)
        .so: (orig, dest) =>
          List(orig, dest),
      blindfold = pov.player.blindfold,
      pref = ctx.pref
    )

  def roundAppPreload(pov: Pov)(using Context) =
    div(cls := "round__app")(
      div(cls := "round__app__board main-board")(povChessground(pov)),
      div(cls := "col1-rmoves-preload")
    )

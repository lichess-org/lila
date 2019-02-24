package views.html.board

import play.api.libs.json.Json
import scala.concurrent.duration.Duration

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object bits {

  def jsData(
    sit: chess.Situation,
    fen: String,
    animationDuration: Duration
  )(implicit ctx: Context) = Json.obj(
    "fen" -> fen.split(" ").take(4).mkString(" "),
    "baseUrl" -> s"$netBaseUrl${routes.Editor.load("")}",
    "color" -> sit.color.letter.toString,
    "castles" -> Json.obj(
      "K" -> (sit canCastle chess.White on chess.KingSide),
      "Q" -> (sit canCastle chess.White on chess.QueenSide),
      "k" -> (sit canCastle chess.Black on chess.KingSide),
      "q" -> (sit canCastle chess.Black on chess.QueenSide)
    ),
    "animation" -> Json.obj(
      "duration" -> ctx.pref.animationFactor * animationDuration.toMillis
    ),
    "is3d" -> ctx.pref.is3d,
    "i18n" -> i18nJsObject(translations)(ctxLang(ctx))
  )

  def domPreload(pov: Option[lila.game.Pov])(implicit ctx: Context) = {
    val theme = ctx.currentTheme
    frag(
      (!ctx.pref.is3d) option raw(s"""<svg version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 0 800 800">
<rect width="800" height="800" fill="#${theme.dark}"/><g fill="#${theme.light}" id="a"><g id="b"><g id="c"><g id="d">
<rect width="100" height="100" id="e"/><use x="200" xlink:href="#e"/></g><use x="400" xlink:href="#d"/></g><use x="100" y="100" xlink:href="#c"/></g><use y="200" xlink:href="#b"/></g><use y="400" xlink:href="#a"/></svg>"""),
      pov.fold(chessgroundSvg)(chessground)
    )
  }

  private val translations = List(
    trans.setTheBoard,
    trans.boardEditor,
    trans.startPosition,
    trans.clearBoard,
    trans.flipBoard,
    trans.loadPosition,
    trans.popularOpenings,
    trans.castling,
    trans.whiteCastlingKingside,
    trans.blackCastlingKingside,
    trans.whitePlays,
    trans.blackPlays,
    trans.variant,
    trans.continueFromHere,
    trans.playWithTheMachine,
    trans.playWithAFriend,
    trans.analysis
  )
}

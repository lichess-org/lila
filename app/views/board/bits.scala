package views.html.board

import play.api.libs.json.Json
import scala.concurrent.duration.Duration

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object bits {

  def jsData(
    sit: draughts.Situation,
    fen: String,
    animationDuration: Duration
  )(implicit ctx: Context) = Json.obj(
    "fen" -> fen.split(":").take(3).mkString(":"),
    "coords" -> ctx.pref.coords,
    "variant" -> sit.board.variant.key,
    "baseUrl" -> s"$netBaseUrl${routes.Editor.parse("")}",
    "color" -> sit.color.letter.toString,
    "animation" -> Json.obj(
      "duration" -> ctx.pref.animationFactor * animationDuration.toMillis
    ),
    "i18n" -> i18nJsObject(translations)(ctxLang(ctx))
  )

  def domPreload(pov: Option[lidraughts.game.Pov])(implicit ctx: Context): Frag = {
    val theme = ctx.currentTheme
    frag(
      raw(s"""<svg class="main-board__preload" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 0 1000 1000">
<rect width="1000" height="1000" fill="#${theme.dark}"/>
<g fill="#${theme.light}" id="a"><g id="b"><rect width="100" height="100" id="c"/>
<use x="200" xlink:href="#c"/><use x="400" xlink:href="#c"/><use x="600" xlink:href="#c"/><use x="800" xlink:href="#c"/>
</g><use x="100" y="100" xlink:href="#b"/></g>
<use y="200" xlink:href="#a"/><use y="400" xlink:href="#a"/><use y="600" xlink:href="#a"/><use y="800" xlink:href="#a"/>
</svg>"""),
      pov.fold(draughtsgroundSvg)(draughtsground)
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
    trans.whitePlays,
    trans.blackPlays,
    trans.variant,
    trans.continueFromHere,
    trans.playWithTheMachine,
    trans.playWithAFriend,
    trans.analysis,
    trans.study
  )
}

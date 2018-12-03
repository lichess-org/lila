package views.html.round

import scalatags.Text.all._

import lila.api.Context
import lila.app.templating.Environment._
import lila.common.HTTPRequest
import lila.common.String.html.{ safeJson, safeJsonValue }
import lila.game.Pov
import lila.i18n.{ I18nKeys => trans }

import controllers.routes

object player {

  def apply(
    pov: Pov,
    data: play.api.libs.json.JsObject,
    tour: Option[lila.tournament.TourMiniView],
    simul: Option[lila.simul.Simul],
    cross: Option[lila.game.Crosstable.WithMatchup],
    playing: List[Pov],
    chatOption: Option[lila.chat.Chat.GameOrEvent],
    bookmarked: Boolean
  )(implicit ctx: Context) = {

    import pov._

    val chatJson = chatOption.map(_.either).map {
      case Left(c) => {
        views.html.chat.restrictedJson(c, name = trans.chatRoom.txt(), timeout = false, withNote = ctx.isAuth, public = false)
      }
      case Right(c) => {
        views.html.chat.json(c.chat, name = trans.chatRoom.txt(), timeout = c.timeout, public = true)
      }
    }

    views.html.round.layout(
      title = s"${trans.play.txt()} ${if (ctx.pref.isZen) "ZEN" else playerText(pov.opponent)} in $gameId",
      side = views.html.game.side(pov, (data \ "game" \ "initialFen").asOpt[String].map(chess.format.FEN), tour.map(_.tour), simul, bookmarked = bookmarked),
      chat = chatOption.map(_ => views.html.chat.html),
      underchat = Some(frag(
        views.html.game.bits.watchers,
        views.html.round.blurs(pov.game),
        views.html.round.holdAlerts(pov.game)
      )),
      moreJs = frag(
        roundTag,
        embedJs(s"""window.customWS = true;
window.onload = function() {
LichessRound.boot({
data: ${safeJsonValue(data)},
i18n: ${jsI18n(pov.game)},
userId: $jsUserId,
${tour.??(t => s"tour: ${toJson(tour.flatMap(_.top).map(lila.tournament.JsonView.top(_, lightUser)))},")}
chat: ${jsOrNull(chatJson)}
}, document.getElementById('lichess'));
};""")
      ),
      moreCss = cssTag("chat.css"),
      openGraph = povOpenGraph(pov).some,
      chessground = false,
      playing = true
    ) {
        frag(
          div(cls := "round cg-512")(
            views.html.board.domPreload(pov.some),
            div(cls := "underboard")(
              div(cls := "center")(
                cross map { c =>
                  div(cls := "crosstable")(
                    views.html.game.crosstable(ctx.userId.fold(c)(c.fromPov), pov.gameId.some)
                  )
                }
              )
            )
          ),
          (playing.nonEmpty || simul.nonEmpty) option
            div(id := "now_playing", cls := List("other_games" -> true, "blindfold" -> ctx.pref.isBlindfold))(
              others(playing, simul)
            )
        )
      }
  }
}

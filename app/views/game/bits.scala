package views.html.game

import play.api.libs.json.Json
import play.twirl.api.Html

import lila.api.Context
import lila.app.templating.Environment._
import lila.game.Pov
import lila.i18n.{ I18nKeys => trans }

object bits {

  def featuredJs(pov: Pov) = Html {
    s"""${gameFenNoCtx(pov, tv = true)}${vstext(pov)(none)}"""
  }

  def mini(pov: Pov)(implicit ctx: Context) = Html {
    s"""${gameFen(pov)}${vstext(pov)(ctx.some)}"""
  }

  def miniBoard(fen: chess.format.FEN, color: chess.Color = chess.White) = Html {
    s"""<div class="mini_board parse_fen is2d" data-color="${color.name}" data-fen="$fen">$miniBoardContent</div>"""
  }

  def watchers(implicit ctx: Context) = Html {
    s"""<div class="watchers hidden"><span class="number">&nbsp;</span> ${trans.spectators.txt().replace(":", "")} <span class="list inline_userlist"></span></div>"""
  }
}

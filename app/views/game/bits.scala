package views.html.game

import play.api.libs.json.Json
import play.twirl.api.Html

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.game.Pov
import lidraughts.i18n.{ I18nKeys => trans }

object bits {

  def featuredJs(pov: Pov) = Html {
    s"""${gameFenNoCtx(pov, tv = true)}${vstext(pov)(none)}"""
  }

  def mini(pov: Pov)(implicit ctx: Context) = Html {
    s"""${gameFen(pov)}${vstext(pov)(ctx.some)}"""
  }

  def miniBoard(fen: draughts.format.FEN, color: draughts.Color = draughts.White) = Html {
    s"""<div class="mini_board parse_fen is2d" data-color="${color.name}" data-fen="$fen">$miniBoardContent</div>"""
  }

  def watchers(implicit ctx: Context) = Html {
    s"""<div class="watchers hidden"><span class="number">&nbsp;</span> ${trans.spectators.txt().replace(":", "")} <span class="list inline_userlist"></span></div>"""
  }

  def gameIcon(game: lidraughts.game.Game): Char = game.perfType match {
    case _ if game.fromPosition => '*'
    case _ if game.imported => '/'
    case Some(p) if game.variant.exotic => p.iconChar
    case _ if game.hasAi => 'n'
    case Some(p) => p.iconChar
    case _ => '8'
  }
}

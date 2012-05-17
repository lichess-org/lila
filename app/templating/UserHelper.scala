package lila
package templating

import play.api.templates.Html

import game.DbPlayer

trait UserHelper {

  def playerLink(player: DbPlayer, cssClass: String) = Html {
    "link " + player.id
  }
}

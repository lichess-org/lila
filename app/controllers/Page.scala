package controllers

import play.api.mvc._, Results._

import lila.app._
import views._

object Page extends LilaController {

  private def bookmark(name: String) = Open { implicit ctx =>
    OptionOk(Prismic oneShotBookmark name) {
      case (doc, resolver) => views.html.site.page(doc, resolver)
    }
  }

  def thanks = bookmark("thanks")

  def tos = bookmark("tos")

  def contribute = bookmark("help")

  def streamHowTo = bookmark("stream-howto")

  def contact = bookmark("contact")

  def master = bookmark("master")

  def kingOfTheHill = bookmark("king-of-the-hill")

  def atomic = bookmark("atomic")

  def antichess = bookmark("antichess")

  def chess960 = bookmark("chess960")

  def horde = bookmark("horde")

  def racingKings = bookmark("racing-kings")

  def crazyhouse = bookmark("crazyhouse")

  def privacy = bookmark("privacy")
}

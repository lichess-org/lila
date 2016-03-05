package controllers

import play.api.mvc._, Results._

import lila.app._
import views._

object Page extends LilaController {

  private def bookmark(name: String) = Open { implicit ctx =>
    OptionOk(Prismic getBookmark name) {
      case (doc, resolver) => views.html.site.page(doc, resolver)
    }
  }

  def thanks = bookmark("thanks")

  def tos = bookmark("tos")

  def contribute = bookmark("help")

  def streamHowTo = bookmark("stream-howto")

  def contact = bookmark("contact")

  def master = bookmark("master")

  def privacy = bookmark("privacy")

  def swag = Open { implicit ctx =>
    OptionOk(Prismic getBookmark "swag") {
      case (doc, resolver) => views.html.site.swag(doc, resolver)
    }
  }

  def variantHome = Open { implicit ctx =>
    OptionOk(Prismic getBookmark "variant") {
      case (doc, resolver) => views.html.site.variantHome(doc, resolver)
    }
  }

  def variant(key: String) = Open { implicit ctx =>
    (for {
      variant <- chess.variant.Variant.byKey get key
      perfType <- lila.rating.PerfType byVariant variant
    } yield OptionOk(Prismic getVariant variant) {
      case (doc, resolver) => views.html.site.variant(doc, resolver, variant, perfType)
    }) | notFound
  }
}

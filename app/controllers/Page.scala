package controllers

import lila.app._
import views._

object Page extends LilaController {

  private def bookmark(name: String) = Open { implicit ctx =>
    pageHit
    OptionOk(Prismic getBookmark name) {
      case (doc, resolver) => views.html.site.page(doc, resolver)
    }
  }

  def thanks = bookmark("thanks")

  def tos = bookmark("tos")

  def contribute = bookmark("help")

  def contact = bookmark("contact")

  def master = bookmark("master")

  def privacy = bookmark("privacy")

  def about = bookmark("about")

  def swag = Open { implicit ctx =>
    pageHit
    OptionOk(Prismic getBookmark "swag") {
      case (doc, resolver) => views.html.site.swag(doc, resolver)
    }
  }

  def variantHome = Open { implicit ctx =>
    import play.api.libs.json._
    negotiate(
      html = OptionOk(Prismic getBookmark "variant") {
        case (doc, resolver) => views.html.site.variantHome(doc, resolver)
      },
      api = _ => Ok(JsArray(chess.variant.Variant.all.map { v =>
        Json.obj(
          "id" -> v.id,
          "key" -> v.key,
          "name" -> v.name
        )
      })).fuccess
    )
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

package controllers

import lidraughts.app._
import views._

object Page extends LidraughtsController {

  def bookmark(name: String) = Open { implicit ctx =>
    OptionOk(Prismic getBookmark name) {
      case (doc, resolver) =>
        views.html.site.page(doc, resolver)
    }
  }

  def swag = Open { implicit ctx =>
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
      api = _ => Ok(JsArray(draughts.variant.Variant.all.map { v =>
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
      variant <- draughts.variant.Variant.byKey get key
      perfType <- lidraughts.rating.PerfType.byVariant(variant).fold(lidraughts.rating.PerfType.checkStandard(variant))(x => x.some)
    } yield OptionOk(Prismic getVariant variant) {
      case (doc, resolver) => views.html.site.variant(doc, resolver, variant, perfType)
    }) | notFound
  }
}

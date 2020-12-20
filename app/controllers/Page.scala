package controllers

import lila.app._

final class Page(
    env: Env,
    prismicC: Prismic
) extends LilaController(env) {

  val thanks  = helpBookmark("thanks")
  val help    = helpBookmark("help")
  val about   = helpBookmark("about")
  val tos     = helpBookmark("tos")
  val privacy = helpBookmark("privacy")
  val master  = helpBookmark("master")
  val ads     = helpBookmark("ads")

  private def helpBookmark(name: String) =
    Open { implicit ctx =>
      pageHit
      OptionOk(prismicC getBookmark name) {
        case (doc, resolver) => views.html.site.help.page(name, doc, resolver)
      }
    }

  val howToCheat = bookmark("how-to-cheat")

  private def bookmark(name: String) =
    Open { implicit ctx =>
      pageHit
      OptionOk(prismicC getBookmark name) {
        case (doc, resolver) => views.html.site.page(doc, resolver)
      }
    }

  def source =
    Open { implicit ctx =>
      pageHit
      OptionOk(prismicC getBookmark "source") {
        case (doc, resolver) => views.html.site.help.source(doc, resolver)
      }
    }

  def variantHome =
    Open { implicit ctx =>
      import play.api.libs.json._
      negotiate(
        html = OptionOk(prismicC getBookmark "variant") {
          case (doc, resolver) => views.html.site.variant.home(doc, resolver)
        },
        api = _ =>
          Ok(JsArray(chess.variant.Variant.all.map { v =>
            Json.obj(
              "id"   -> v.id,
              "key"  -> v.key,
              "name" -> v.name
            )
          })).fuccess
      )
    }

  def variant(key: String) =
    Open { implicit ctx =>
      (for {
        variant  <- chess.variant.Variant.byKey get key
        perfType <- lila.rating.PerfType byVariant variant
      } yield OptionOk(prismicC getVariant variant) {
        case (doc, resolver) => views.html.site.variant.show(doc, resolver, variant, perfType)
      }) | notFound
    }
}

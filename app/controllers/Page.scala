package controllers

import lila.app._

final class Page(
    env: Env,
    prismicC: Prismic
) extends LilaController(env) {

  val thanks  = helpDocument("thanks")
  val help    = helpDocument("help")
  val about   = helpDocument("about")
  val tos     = helpDocument("tos")
  val privacy = helpDocument("privacy")
  val source = helpDocument("source")
  val master  = helpDocument("master")
  val ads     = helpDocument("ads")
  val patron  = singleDocument("patron")
  val notSupported = singleDocument("404")

  private def helpBookmark(name: String) =
    Open { implicit ctx =>
      pageHit
      OptionOk(prismicC getBookmark name) {
        case (doc, resolver) => views.html.site.help.page(name, doc, resolver)
      }
    }

  private def helpDocument(uid: String) =
    Open { implicit ctx =>
      pageHit
      OptionOk(prismicC.getPage("doc", uid)) {
        case (doc, resolver) => {
          views.html.site.help.page(uid, doc, resolver)
          }
      }
    }

  private def singleDocument(uid: String) = 
    Open { implicit ctx =>
      pageHit
      OptionOk(prismicC.getPage("doc", uid)) {
        case (doc, resolver) => {
          views.html.site.page(doc, resolver)
          }
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

package controllers

import lila.app._
import lila.blog.BlogLang

final class Page(
    env: Env,
    prismicC: Prismic
) extends LilaController(env) {

  val thanks    = helpDocument("thanks")
  val resources = helpDocument("resources")
  val help      = helpDocument("help")
  val about     = helpDocument("about")
  val tos       = helpDocument("tos")
  val privacy   = helpDocument("privacy")
  val ads       = helpDocument("ads")
  val donations = helpDocument("donations")

  // Explanations use lang
  val storm   = explanation("storm")
  val impasse = explanation("impasse")
  val tsume   = explanation("tsume")

  val kif = notationExplanation("kif")
  val csa = notationExplanation("csa")

  private def helpDocument(uid: String) =
    Open { implicit ctx =>
      pageHit
      OptionOk(prismicC.getPage("doc", uid)) {
        case (doc, resolver) => {
          views.html.site.help.page(uid, doc, resolver)
        }
      }
    }

  private def bookmark(name: String) =
    Open { implicit ctx =>
      pageHit
      OptionOk(prismicC getBookmark name) { case (doc, resolver) =>
        views.html.site.page(doc, resolver)
      }
    }

  def loneBookmark(name: String) = bookmark(name)

  private def explanation(uid: String) =
    Open { implicit ctx =>
      pageHit
      OptionOk(prismicC.getPage("doc", uid, BlogLang.fromLang(ctx.lang))) { case (doc, resolver) =>
        views.html.site.page(doc, resolver, lila.i18n.LangList.EnglishJapanese.some)
      }
    }

  private def notationExplanation(uid: String) =
    Open { implicit ctx =>
      pageHit
      OptionOk(prismicC.getPage("doc", uid, BlogLang.fromLang(ctx.lang))) { case (doc, _) =>
        views.html.site.notationExplanation(doc)
      }
    }

  def source =
    Open { implicit ctx =>
      pageHit
      OptionOk(prismicC.getPage("doc", "source")) { case (doc, resolver) =>
        views.html.site.help.source(doc, resolver)
      }
    }

  def variantHome =
    Open { implicit ctx =>
      import play.api.libs.json._
      negotiate(
        html = Ok(views.html.site.variant.home).fuccess,
        api = _ =>
          Ok(JsArray(shogi.variant.Variant.all.map { v =>
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
        variant <- shogi.variant.Variant.byKey get key
      } yield OptionOk(prismicC.getVariant(variant, BlogLang.fromLangCode(ctx.lang.code))) {
        case (doc, resolver) =>
          views.html.site.variant.show(doc, resolver, variant)
      }) | notFound
    }
}

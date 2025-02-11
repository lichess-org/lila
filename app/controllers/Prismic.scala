package controllers

import lila.app._
import lila.blog.BlogLang
import lila.prismic.DocumentLinkResolver
import lila.prismic.Predicate

final class Prismic(
    env: Env,
)(implicit ws: play.api.libs.ws.WSClient)
    extends LilaController(env) {

  private val logger = lila.log("prismic")

  private def prismic = env.prismic.prismic

  val thanks    = helpDocument("thanks")
  val resources = helpDocument("resources")
  val help      = helpDocument("help")
  val about     = helpDocument("about")
  val tos       = helpDocument("tos")
  val privacy   = helpDocument("privacy")
  val ads       = helpDocument("ads")
  val donations = helpDocument("donations")

  private def helpDocument(uid: String) =
    Open { implicit ctx =>
      pageHit
      OptionOk(getPage("doc", uid)) {
        case (doc, resolver) => {
          views.html.site.help.page(uid, doc, resolver)
        }
      }
    }

  def page(uid: String) =
    Open { implicit ctx =>
      pageHit
      OptionOk(getPage("doc", uid, BlogLang.fromLang(ctx.lang))) { case (doc, resolver) =>
        uid match {
          case "kif" | "csa" =>
            views.html.site.notationExplanation(doc)
          case _ => views.html.site.page(doc, resolver, lila.i18n.LangList.EnglishJapanese.some)
        }
      }
    }

  def bcPage(uid: String) =
    Action {
      if (List("storm", "impasse", "tsume", "kif", "csa").contains(uid))
        MovedPermanently(routes.Prismic.page(uid).url)
      else NotFound
    }

  def source =
    Open { implicit ctx =>
      pageHit
      OptionOk(getPage("doc", "source")) { case (doc, resolver) =>
        views.html.site.help.source(doc, resolver)
      }
    }

  def variantHome =
    Open { implicit ctx =>
      import play.api.libs.json._
      negotiate(
        html = Ok(views.html.site.variant.home).fuccess,
        json = Ok(JsArray(shogi.variant.Variant.all.map { v =>
          Json.obj(
            "id"   -> v.id,
            "key"  -> v.key,
            "name" -> v.name,
          )
        })).fuccess,
      )
    }

  def variant(key: String) =
    Open { implicit ctx =>
      (for {
        variant <- shogi.variant.Variant.byKey get key
      } yield OptionOk(
        getPage("variant", variant.key, BlogLang.fromLangCode(ctx.lang.code)),
      ) { case (doc, resolver) =>
        views.html.site.variant.show(doc, resolver, variant)
      }) | notFound
    }

  private def getDocumentByUID(customType: String, uid: String, lang: BlogLang) =
    prismic.get flatMap { api =>
      api.search
        .set("lang", lang.code)
        .query(Predicate.at(s"document.type", customType), Predicate.at(s"my.$customType.uid", uid))
        .ref(api.master.ref)
        .submit() dmap {
        _.results.headOption
      }
    }

  def getPage(customType: String, uid: String, lang: BlogLang = BlogLang.default) =
    getDocumentByUID(customType, uid, lang) map2 { (doc: lila.prismic.Document) =>
      doc -> Prismic.documentLinkResolver
    } recover { case e: Exception =>
      logger.error(s"page:$uid", e)
      none
    }

}

private[controllers] object Prismic {
  implicit val documentLinkResolver: DocumentLinkResolver =
    DocumentLinkResolver { link =>
      link.typ match {
        case "blog" =>
          routes.Blog.show(link.id).url
        case _ =>
          link.uid.fold(routes.Lobby.home.url) { uid =>
            routes.Prismic.page(uid).url
          }
      }
    }

}

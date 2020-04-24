package controllers

import scala.concurrent.duration._

import io.prismic.Fragment.DocumentLink
import io.prismic.{ Api => PrismicApi, _ }
import draughts.variant.Variant
import lidraughts.app._
import lidraughts.common.Lang

object Prismic {

  private type DocWithResolver = (Document, AnyRef with DocumentLinkResolver)

  private val logger = lidraughts.log("prismic")

  val prismicLogger = (level: Symbol, message: String) => level match {
    case 'DEBUG => logger debug message
    case 'ERROR => logger error message
    case _ => logger info message
  }

  private val prismicApiCache = Env.memo.asyncCache.single[PrismicApi](
    name = "prismic.fetchPrismicApi",
    f = PrismicApi.get(Env.api.PrismicApiUrl, logger = prismicLogger),
    expireAfter = _.ExpireAfterWrite(1 minute)
  )

  private val variantLanguageCache = Env.memo.asyncCache.clearable[Variant, Option[List[DocWithResolver]]](
    name = "prismic.variantLanguageCache",
    f = fetchVariantLanguages,
    expireAfter = _.ExpireAfterWrite(10 minutes)
  )

  private def fetchVariantLanguages(variant: Variant) = prismicApi flatMap { api =>
    api.forms("variant")
      .query(s"""[[:d = at(my.variant.key, "${variant.key}")]]""")
      .set("lang", "*")
      .ref(api.master.ref)
      .submit() map {
        _.results.map(_ -> makeLinkResolver(api)).some
      }
  } recover {
    case e: Exception =>
      logger.error(s"variant:${variant.key}", e)
      lidraughts.mon.http.prismic.timeout()
      none
  }

  private def invalidateVariantLanguages(variant: Variant) = {
    variantLanguageCache.invalidate(variant)
    none[DocWithResolver]
  }

  def prismicApi = prismicApiCache.get

  implicit def makeLinkResolver(prismicApi: PrismicApi, ref: Option[String] = None) =
    DocumentLinkResolver(prismicApi) {
      case (link, _) => routes.Blog.show(link.id, link.slug, ref).url
      case _ => routes.Lobby.home.url
    }

  def getDocument(id: String): Fu[Option[Document]] = prismicApi flatMap { api =>
    api.forms("everything")
      .query(s"""[[:d = at(document.id, "$id")]]""")
      .ref(api.master.ref)
      .submit() map {
        _.results.headOption
      }
  }

  def getBookmark(name: String) = prismicApi flatMap { api =>
    api.bookmarks.get(name) ?? getDocument map2 { (doc: io.prismic.Document) =>
      doc -> makeLinkResolver(api)
    }
  } recover {
    case e: Exception =>
      logger.error(s"bookmark:$name", e)
      lidraughts.mon.http.prismic.timeout()
      none
  }

  def getVariant(variant: Variant, lang: Lang) = variantLanguageCache get variant map {
    _.fold(invalidateVariantLanguages(variant)) { docs =>
      def findLang(l: String) = docs.find(doc => ~doc._1.getText("variant.lang").map(_.startsWith(l)))
      findLang(lang.language) orElse findLang("en")
    }
  }
}

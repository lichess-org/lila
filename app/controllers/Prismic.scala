package controllers

import io.prismic.Fragment.DocumentLink
import io.prismic.{ Api => PrismicApi, _ }
import lila.app._

object Prismic {

  private val cache = BuiltInCache(200)

  private val logger = (level: Symbol, message: String) => level match {
    case 'DEBUG => play.api.Logger("prismic") debug message
    case 'ERROR => play.api.Logger("prismic") error message
    case _      => play.api.Logger("prismic") info message
  }

  def fetchPrismicApi =
    PrismicApi.get(Env.api.PrismicApiUrl, cache = cache, logger = logger)

  implicit def makeLinkResolver(prismicApi: PrismicApi, ref: Option[String] = None) =
    DocumentLinkResolver(prismicApi) {
      case (DocumentLink(id, _, _, slug, false), _) => routes.Blog.show(id, slug, ref).url
      case _                                        => routes.Lobby.home.url
    }

  def getBookmark(prismicApi: PrismicApi)(name: String): Fu[Option[Document]] =
    prismicApi.bookmarks.get(name) ?? getDocument(prismicApi)

  def getDocument(prismicApi: PrismicApi)(id: String): Fu[Option[Document]] =
    prismicApi.forms("everything")
      .query(s"""[[:d = at(document.id, "$id")]]""")
      .ref(prismicApi.master.ref)
      .submit() map {
        _.results.headOption
      }

  def oneShotBookmark(name: String) = fetchPrismicApi flatMap { api =>
    getBookmark(api)(name) map2 { (doc: io.prismic.Document) =>
      doc -> makeLinkResolver(api)
    }
  }
}

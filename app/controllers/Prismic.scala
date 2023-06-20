package controllers

import io.prismic.{ Api as PrismicApi, * }
import lila.app.{ given, * }

final class Prismic(env: Env)(using Executor, play.api.libs.ws.StandaloneWSClient):

  private val logger = lila.log("prismic")

  import env.blog.api.prismicApi

  type MakeLinkResolver = (PrismicApi, Option[String]) => DocumentLinkResolver
  given makeLinkResolver: MakeLinkResolver = (prismicApi, ref) =>
    DocumentLinkResolver(prismicApi): (link, _) =>
      routes.Blog.show(link.id, link.slug, ref).url

  private def getDocument(id: String): Fu[Option[Document]] =
    lila.blog.BlogApi.looksLikePrismicId(id) so
      prismicApi.flatMap: api =>
        api
          .forms("everything")
          .query(s"""[[:d = at(document.id, "$id")]]""")
          .ref(api.master.ref)
          .submit() dmap {
          _.results.headOption
        }

  def getBookmark(name: String) =
    prismicApi
      .flatMap: api =>
        api.bookmarks.get(name) so getDocument map2 { doc =>
          doc -> makeLinkResolver(api, none)
        }
      .recover { case e: Exception =>
        logger.error(s"bookmark:$name", e)
        none
      }

  def getVariant(variant: chess.variant.Variant) =
    prismicApi.flatMap: api =>
      api
        .forms("variant")
        .query(s"""[[:d = at(my.variant.key, "${variant.key}")]]""")
        .ref(api.master.ref)
        .submit()
        .map:
          _.results.headOption.map(_ -> makeLinkResolver(api, none))

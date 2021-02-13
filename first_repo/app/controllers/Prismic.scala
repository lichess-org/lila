package controllers

import io.prismic.{ Api => PrismicApi, _ }
import lila.app._

final class Prismic(
    env: Env
)(implicit ec: scala.concurrent.ExecutionContext, ws: play.api.libs.ws.StandaloneWSClient) {

  private val logger = lila.log("prismic")

  private def prismicApi = env.blog.api.prismicApi

  implicit def makeLinkResolver(prismicApi: PrismicApi, ref: Option[String] = None) =
    DocumentLinkResolver(prismicApi) {
      case (link, _) => routes.Blog.show(link.id, link.slug, ref).url
      case _         => routes.Lobby.home.url
    }

  private def getDocument(id: String): Fu[Option[Document]] =
    prismicApi flatMap { api =>
      api
        .forms("everything")
        .query(s"""[[:d = at(document.id, "$id")]]""")
        .ref(api.master.ref)
        .submit() dmap {
        _.results.headOption
      }
    }

  def getBookmark(name: String) =
    prismicApi flatMap { api =>
      api.bookmarks.get(name) ?? getDocument map2 { (doc: io.prismic.Document) =>
        doc -> makeLinkResolver(api)
      }
    } recover { case e: Exception =>
      logger.error(s"bookmark:$name", e)
      none
    }

  def getVariant(variant: chess.variant.Variant) =
    prismicApi flatMap { api =>
      api
        .forms("variant")
        .query(s"""[[:d = at(my.variant.key, "${variant.key}")]]""")
        .ref(api.master.ref)
        .submit() map {
        _.results.headOption map (_ -> makeLinkResolver(api))
      }
    }
}

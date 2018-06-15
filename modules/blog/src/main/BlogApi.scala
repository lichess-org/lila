package lila.blog

import io.prismic._
import scala.concurrent.duration._

import lila.common.MaxPerPage
import lila.common.paginator._

final class BlogApi(
    asyncCache: lila.memo.AsyncCache.Builder,
    prismicUrl: String,
    collection: String
) {

  def recent(api: Api, ref: Option[String], page: Int, maxPerPage: MaxPerPage): Fu[Option[Paginator[Document]]] =
    api.forms(collection).ref(resolveRef(api)(ref) | api.master.ref)
      .orderings(s"[my.$collection.date desc]")
      .pageSize(maxPerPage.value).page(page).submit().fold(_ => none, some _) map2 { (res: Response) =>
        PrismicPaginator(res, page, maxPerPage)
      }

  def one(api: Api, ref: Option[String], id: String) =
    api.forms(collection)
      .query(s"""[[:d = at(document.id, "$id")]]""")
      .ref(resolveRef(api)(ref) | api.master.ref).submit() map (_.results.headOption)

  // -- Build a Prismic context
  def context(refName: Option[String])(implicit linkResolver: (Api, Option[String]) => DocumentLinkResolver) =
    prismicApi map { api =>
      val ref = resolveRef(api)(refName)
      BlogApi.Context(api, ref | api.master.ref, linkResolver(api, ref))
    }

  private def resolveRef(api: Api)(ref: Option[String]) =
    ref.map(_.trim).filterNot(_.isEmpty).flatMap { reqRef =>
      api.refs.values.collectFirst {
        case r if r.label == reqRef => r.ref
        case r if r.ref == reqRef => r.ref
      }
    }

  private val cache = BuiltInCache(200)
  private val prismicLogger = (level: Symbol, message: String) => level match {
    case 'DEBUG => logger debug message
    case 'ERROR => logger error message
    case _ => logger info message
  }

  private val fetchPrismicApi = asyncCache.single[Api](
    name = "blogApi.fetchPrismicApi",
    f = Api.get(prismicUrl, cache = cache, logger = prismicLogger),
    expireAfter = _.ExpireAfterWrite(15 seconds)
  )

  def prismicApi = fetchPrismicApi.get
}

object BlogApi {

  def extract(body: Fragment.StructuredText): String =
    body.blocks
      .takeWhile(_.isInstanceOf[Fragment.StructuredText.Block.Paragraph])
      .take(2).map {
        case Fragment.StructuredText.Block.Paragraph(text, _, _) => s"<p>$text</p>"
        case _ => ""
      }.mkString

  case class Context(api: Api, ref: String, linkResolver: DocumentLinkResolver) {
    def maybeRef = Option(ref).filterNot(_ == api.master.ref)
  }
}

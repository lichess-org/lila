package lila.blog

import io.prismic._
import play.api.mvc.RequestHeader
import scala.concurrent.duration._

import lila.common.MaxPerPage
import lila.common.paginator._

final class BlogApi(
    asyncCache: lila.memo.AsyncCache.Builder,
    prismicUrl: String,
    collection: String
) {

  def recent(api: Api, ref: Option[String], page: Int, maxPerPage: MaxPerPage): Fu[Option[Paginator[Document]]] =
    api.forms(collection).ref(ref | api.master.ref)
      .orderings(s"[my.$collection.date desc]")
      .pageSize(maxPerPage.value).page(page).submit().fold(_ => none, some _) map2 { (res: Response) =>
        PrismicPaginator(res, page, maxPerPage)
      }
  def recent(prismic: BlogApi.Context, page: Int, maxPerPage: MaxPerPage): Fu[Option[Paginator[Document]]] =
    recent(prismic.api, prismic.ref.some, page, maxPerPage)

  def one(api: Api, ref: Option[String], id: String): Fu[Option[Document]] =
    api.forms(collection)
      .query(s"""[[:d = at(document.id, "$id")]]""")
      .ref(ref | api.master.ref).submit() map (_.results.headOption)

  def one(prismic: BlogApi.Context, id: String): Fu[Option[Document]] = one(prismic.api, prismic.ref.some, id)

  def byYear(prismic: BlogApi.Context, year: Int): Fu[List[MiniPost]] = {
    prismic.api.forms(collection).ref(prismic.ref)
      .query(s"[[date.year(my.$collection.date, $year)]]")
      .orderings(s"[my.$collection.date desc]")
      .pageSize(100) // prismic max
      .submit().fold(_ => Nil, _.results flatMap MiniPost.fromDocument(collection, "wide"))
  }

  def context(req: RequestHeader)(implicit linkResolver: (Api, Option[String]) => DocumentLinkResolver): Fu[BlogApi.Context] = {
    prismicApi map { api =>
      val ref = resolveRef(api) {
        req.cookies.get(Prismic.previewCookie).map(_.value)
          .orElse(req.queryString get "ref" flatMap (_.headOption) filter (_.nonEmpty))
      }
      BlogApi.Context(api, ref | api.master.ref, linkResolver(api, ref))
    }
  }

  private def resolveRef(api: Api)(ref: Option[String]) =
    ref.map(_.trim).filterNot(_.isEmpty) map { reqRef =>
      api.refs.values.collectFirst {
        case r if r.label == reqRef => r.ref
      } getOrElse reqRef
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

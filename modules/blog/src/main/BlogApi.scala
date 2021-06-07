package lila.blog

import io.prismic._
import play.api.mvc.RequestHeader
import play.api.libs.ws.StandaloneWSClient

import lila.common.config.MaxPerPage
import lila.common.paginator._

final class BlogApi(
    config: BlogConfig
)(implicit ec: scala.concurrent.ExecutionContext, ws: StandaloneWSClient) {

  private def collection = config.collection

  def recent(
      api: Api,
      page: Int,
      maxPerPage: MaxPerPage,
      ref: Option[String]
  ): Fu[Option[Paginator[Document]]] =
    api
      .forms(collection)
      .ref(ref | api.master.ref)
      .orderings(s"[my.$collection.date desc]")
      .pageSize(maxPerPage.value)
      .page(page)
      .submit()
      .fold(_ => none, some)
      .dmap2 { PrismicPaginator(_, page, maxPerPage) }

  def recent(
      prismic: BlogApi.Context,
      page: Int,
      maxPerPage: MaxPerPage
  ): Fu[Option[Paginator[Document]]] =
    recent(prismic.api, page, maxPerPage, prismic.ref.some)

  def one(api: Api, ref: Option[String], id: String): Fu[Option[Document]] =
    api
      .forms(collection)
      .query(s"""[[:d = at(document.id, "$id")]]""")
      .ref(ref | api.master.ref)
      .submit()
      .map(_.results.headOption)

  def one(prismic: BlogApi.Context, id: String): Fu[Option[Document]] = one(prismic.api, prismic.ref.some, id)

  def byYear(prismic: BlogApi.Context, year: Int): Fu[List[MiniPost]] = {
    prismic.api
      .forms(collection)
      .ref(prismic.ref)
      .query(s"[[date.year(my.$collection.date, $year)]]")
      .orderings(s"[my.$collection.date desc]")
      .pageSize(100) // prismic max
      .submit()
      .fold(_ => Nil, _.results flatMap MiniPost.fromDocument(collection, "wide"))
  }

  def context(
      req: RequestHeader
  )(implicit linkResolver: (Api, Option[String]) => DocumentLinkResolver): Fu[BlogApi.Context] = {
    prismicApi map { api =>
      val ref = resolveRef(api) {
        req.cookies
          .get(Prismic.previewCookie)
          .map(_.value)
          .orElse(req.queryString get "ref" flatMap (_.headOption) filter (_.nonEmpty))
      }
      BlogApi.Context(api, ref | api.master.ref, linkResolver(api, ref))
    }
  }

  def masterContext(implicit
      linkResolver: (Api, Option[String]) => DocumentLinkResolver
  ): Fu[BlogApi.Context] = {
    prismicApi map { api =>
      BlogApi.Context(api, api.master.ref, linkResolver(api, none))
    }
  }

  def all(page: Int = 1)(implicit prismic: BlogApi.Context): Fu[List[Document]] =
    recent(prismic.api, page, MaxPerPage(50), none) flatMap { res =>
      val docs = res.??(_.currentPageResults).toList
      (docs.nonEmpty ?? all(page + 1)) map (docs ::: _)
    }

  private def resolveRef(api: Api)(ref: Option[String]) =
    ref.map(_.trim).filterNot(_.isEmpty) map { reqRef =>
      api.refs.values.collectFirst {
        case r if r.label == reqRef => r.ref
      } getOrElse reqRef
    }

  private val prismicBuilder = new Prismic

  def prismicApi = prismicBuilder.get(config.apiUrl)
}

object BlogApi {

  def extract(body: Fragment.StructuredText): String =
    body.blocks
      .takeWhile(_.isInstanceOf[Fragment.StructuredText.Block.Paragraph])
      .take(2)
      .map {
        case Fragment.StructuredText.Block.Paragraph(text, _, _) => s"<p>$text</p>"
        case _                                                   => ""
      }
      .mkString

  case class Context(api: Api, ref: String, linkResolver: DocumentLinkResolver) {
    def maybeRef = Option(ref).filterNot(_ == api.master.ref)
  }
}

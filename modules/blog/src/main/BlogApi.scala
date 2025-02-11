package lila.blog

import play.api.libs.ws.WSClient

import lila.common.config.MaxPerPage
import lila.common.paginator._
import lila.prismic._

final class BlogApi(
    config: BlogConfig,
    prismic: lila.prismic.Prismic,
)(implicit ec: scala.concurrent.ExecutionContext, ws: WSClient) {

  private def collection = config.collection

  def recent(
      api: Api,
      page: Int,
      maxPerPage: MaxPerPage,
      lang: BlogLang,
  ): Fu[Option[Paginator[Document]]] =
    api.search
      .set("lang", lang.code)
      .ref(api.master.ref)
      .orderings(s"[my.$collection.date desc]")
      .pageSize(maxPerPage.value)
      .page(page)
      .submit()
      .fold(_ => none, some _)
      .dmap2 { PrismicPaginator(_, page, maxPerPage) }

  def one(api: Api, id: String): Fu[Option[FullPost]] =
    api.search
      .set("lang", "*")
      .query(Predicate.at("document.id", id))
      .ref(api.master.ref)
      .submit() map (_.results.flatMap(doc => FullPost.fromDocument(collection)(doc)).headOption)

  def one(prismic: BlogApi.Context, id: String): Fu[Option[FullPost]] =
    one(prismic.api, id)

  def latest(prismic: BlogApi.Context, lang: BlogLang): Fu[Option[FullPost]] =
    prismic.api.search
      .set("lang", lang.code)
      .ref(prismic.api.master.ref)
      .orderings(s"[my.$collection.date desc]")
      .pageSize(1)
      .submit() map (_.results.flatMap(doc => FullPost.fromDocument(collection)(doc)).headOption)

  def byYear(prismic: BlogApi.Context, year: Int, lang: BlogLang): Fu[List[MiniPost]] =
    prismic.api.search
      .set("lang", lang.code)
      .ref(prismic.api.master.ref)
      .query(
        Predicate.at("document.type", collection),
        Predicate.year(s"my.$collection.date", year),
      )
      .orderings(s"[my.$collection.date desc]")
      .pageSize(100) // prismic maximum
      .submit()
      .fold(_ => Nil, _.results flatMap MiniPost.fromDocument(collection, "wide"))

  def context(implicit linkResolver: DocumentLinkResolver): Fu[BlogApi.Context] =
    prismic.get map { api =>
      BlogApi.Context(api, linkResolver)
    }

  def masterContext(implicit
      linkResolver: DocumentLinkResolver,
  ): Fu[BlogApi.Context] =
    prismic.get map { api =>
      BlogApi.Context(api, linkResolver)
    }

  def all(page: Int = 1)(implicit prismic: BlogApi.Context): Fu[List[Document]] =
    recent(prismic.api, page, MaxPerPage(50), BlogLang.All) flatMap { res =>
      val docs = res.??(_.currentPageResults).toList
      (docs.nonEmpty ?? all(page + 1)) map (docs ::: _)
    }

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

  case class Context(api: Api, linkResolver: DocumentLinkResolver)
}

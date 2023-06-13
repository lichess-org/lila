package lila.blog

import scala.util.Try
import io.prismic.*
import play.api.mvc.RequestHeader
import play.api.libs.ws.StandaloneWSClient

import lila.common.Bus
import lila.common.config.MaxPerPage
import lila.common.paginator.*
import lila.hub.actorApi.lpv.GamePgnsFromText
import chess.format.pgn.PgnStr

final class BlogApi(
    config: BlogConfig,
    cacheApi: lila.memo.CacheApi
)(using Executor, Scheduler, StandaloneWSClient):

  import BlogApi.looksLikePrismicId

  private def collection = config.collection

  def recent(
      api: Api,
      page: Int,
      maxPerPage: MaxPerPage,
      ref: Option[String]
  ): Fu[Option[Paginator[Document]]] = Try {
    api
      .forms(collection)
      .ref(ref | api.master.ref)
      .orderings(s"[my.$collection.date desc]")
      .pageSize(maxPerPage.value)
      .page(page)
      .submit()
      .fold(_ => none, some)
      .dmap2 { PrismicPaginator(_, page, maxPerPage) }
  } recover { case _: NoSuchElementException =>
    fuccess(none)
  } get

  def recent(
      prismic: BlogApi.Context,
      page: Int,
      maxPerPage: MaxPerPage
  ): Fu[Option[Paginator[Document]]] =
    recent(prismic.api, page, maxPerPage, prismic.ref.some)

  def one(api: Api, ref: Option[String], id: String): Fu[Option[Document]] =
    looksLikePrismicId(id) so api
      .forms(collection)
      .query(s"""[[:d = at(document.id, "$id")]]""")
      .ref(ref | api.master.ref)
      .submit()
      .map(_.results.headOption)

  def one(prismic: BlogApi.Context, id: String): Fu[Option[Document]] =
    one(prismic.api, prismic.ref.some, id).flatMapz { doc =>
      doc.getHtml("blog.body", prismic.linkResolver) match
        case Some(html) =>
          Bus
            .ask("lpv")(GamePgnsFromText(html, _))
            .map(pgnCache.putAll) inject doc.some
        case _ => fuccess(doc.some)
    }

  def byYear(prismic: BlogApi.Context, year: Int): Fu[List[MiniPost]] =
    prismic.api
      .forms(collection)
      .ref(prismic.ref)
      .query(s"[[date.year(my.$collection.date, $year)]]")
      .orderings(s"[my.$collection.date desc]")
      .pageSize(100) // prismic max
      .submit()
      .fold(_ => Nil, _.results flatMap MiniPost.fromDocument(collection, "wide"))

  def context(
      req: RequestHeader
  )(using linkResolver: (Api, Option[String]) => DocumentLinkResolver): Fu[BlogApi.Context] =
    prismicApi map { api =>
      val ref = resolveRef(api) {
        req.cookies
          .get(Prismic.previewCookie)
          .map(_.value)
          .orElse(req.queryString get "ref" flatMap (_.headOption) filter (_.nonEmpty))
      }
      BlogApi.Context(api, ref | api.master.ref, linkResolver(api, ref))
    }

  def masterContext(using
      linkResolver: (Api, Option[String]) => DocumentLinkResolver
  ): Fu[BlogApi.Context] =
    prismicApi map { api =>
      BlogApi.Context(api, api.master.ref, linkResolver(api, none))
    }

  def all(page: Int = 1)(using prismic: BlogApi.Context): Fu[List[Document]] =
    recent(prismic.api, page, MaxPerPage(50), none) flatMap { res =>
      val docs = res.so(_.currentPageResults).toList
      (docs.nonEmpty so all(page + 1)) map (docs ::: _)
    }

  def expand(html: Html) = Html(
    expandGameRegex.replaceAllIn(
      html.value,
      m =>
        pgnCache.getIfPresent(GameId(m.group(1))).fold(m.matched) { pgn =>
          val esc   = lila.common.base.StringUtils.escapeHtmlRaw(pgn.value)
          val color = Option(m.group(2)).getOrElse("white")
          val ply   = Option(m.group(3)).getOrElse("last")
          s"""<div class="lpv--autostart" data-pgn="$esc" data-orientation="$color" data-ply="$ply"></div>"""
        }
    )
  )

  // match the entire <a.../> tag with scheme & domain.  href value should be identical to inner text
  private val expandGameRegex =
    """<a href="https://lichess\.org/(\w{8})(?:/(white|black)|\w{4}|)(?:#(last|\d+))?">https://lichess\.org/[^<]{8,19}+</a>""".r

  private val pgnCache = cacheApi.notLoadingSync[GameId, PgnStr](256, "prisblog.markup.pgn") {
    _.expireAfterWrite(1 second).build()
  }

  private def resolveRef(api: Api)(ref: Option[String]) =
    ref.map(_.trim).filterNot(_.isEmpty) map { reqRef =>
      api.refs.values.collectFirst {
        case r if r.label == reqRef => r.ref
      } getOrElse reqRef
    }

  def prismicApi = (new Prismic).get(config.apiUrl)

object BlogApi:

  def extract(body: Fragment.StructuredText): String =
    body.blocks
      .takeWhile(_.isInstanceOf[Fragment.StructuredText.Block.Paragraph])
      .take(2)
      .map {
        case Fragment.StructuredText.Block.Paragraph(text, _, _) => s"<p>$text</p>"
        case _                                                   => ""
      }
      .mkString

  case class Context(api: Api, ref: String, linkResolver: DocumentLinkResolver):
    def maybeRef = Option(ref).filterNot(_ == api.master.ref)

  private val idRegex = """^[\w-]{16}$""".r

  def looksLikePrismicId(id: String) = idRegex.matches(id)

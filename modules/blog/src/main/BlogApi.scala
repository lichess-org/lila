package lila.blog

import scala.util.Try
import io.prismic.*
import play.api.mvc.RequestHeader
import play.api.libs.ws.StandaloneWSClient
import java.util.regex.{ Pattern, Matcher }

import lila.common.{ Bus, LpvEmbed }
import lila.common.config.{ BaseUrl, MaxPerPage }
import lila.common.paginator.*
import lila.hub.actorApi.lpv.AllPgnsFromText
import chess.format.pgn.PgnStr

final class BlogApi(
    config: BlogConfig,
    baseUrl: BaseUrl,
    cacheApi: lila.memo.CacheApi
)(using Executor, Scheduler, StandaloneWSClient):

  import BlogApi.looksLikePrismicId
  import config.collection

  def recent(
      api: Api,
      page: Int,
      maxPerPage: MaxPerPage,
      ref: Option[String]
  ): Fu[Option[Paginator[BlogPost]]] = Try {
    api
      .forms(collection)
      .ref(ref | api.master.ref)
      .orderings(s"[my.$collection.date desc]")
      .pageSize(maxPerPage.value)
      .page(page)
      .submit()
      .fold(_ => none, some)
      .map2 { PrismicPaginator(_, page, maxPerPage) }
      .map2 { _.mapResults(BlogPost(_)) }
  }.recover { case _: NoSuchElementException =>
    fuccess(none)
  }.get

  def recent(
      prismic: BlogApi.Context,
      page: Int,
      maxPerPage: MaxPerPage
  ): Fu[Option[Paginator[BlogPost]]] =
    recent(prismic.api, page, maxPerPage, prismic.ref.some)

  def one(api: Api, ref: Option[String], id: String): Fu[Option[BlogPost]] =
    looksLikePrismicId(id) so api
      .forms(collection)
      .query(s"""[[:d = at(document.id, "$id")]]""")
      .ref(ref | api.master.ref)
      .submit()
      .map(_.results.headOption.map(BlogPost(_)))

  def one(prismic: BlogApi.Context, id: String): Fu[Option[BlogPost]] =
    one(prismic.api, prismic.ref.some, id).flatMapz { doc =>
      doc.getHtml("blog.body", prismic.linkResolver) match
        case Some(html) =>
          Bus
            .ask("lpv")(AllPgnsFromText(html, _))
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
      .fold(_ => Nil, _.results.map(BlogPost(_)).flatMap(MiniPost.apply))

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
    prismicApi.map: api =>
      BlogApi.Context(api, api.master.ref, linkResolver(api, none))

  def all(page: Int = 1)(using prismic: BlogApi.Context): Fu[List[BlogPost]] =
    recent(prismic.api, page, MaxPerPage(50), none) flatMap { res =>
      val docs = res.so(_.currentPageResults).toList
      (docs.nonEmpty so all(page + 1)).map(docs ::: _)
    }

  def expand(html: Html) = html.map(expandGames).map(expandChapters)

  private def expandGames(html: String) = expandGameRegex.replaceAllIn(
    html,
    m =>
      pgnCache.getIfPresent(m.group(1)) match
        case Some(LpvEmbed.PublicPgn(pgn)) =>
          val esc = Matcher.quoteReplacement:
            lila.common.base.StringUtils.escapeHtmlRaw(pgn.value)
          val color = Option(m.group(2)).getOrElse("white")
          val ply   = Option(m.group(3)).getOrElse("last")
          s"""<div class="lpv--autostart" data-pgn="$esc" data-orientation="$color" data-ply="$ply"></div>"""
        case _ => m.matched
  )

  private def expandChapters(html: String) = expandChapterRegex.replaceAllIn(
    html,
    m =>
      pgnCache.getIfPresent(m.group(1)) match
        case Some(LpvEmbed.PublicPgn(pgn)) =>
          val esc = Matcher.quoteReplacement:
            lila.common.base.StringUtils.escapeHtmlRaw(pgn.value)
          val ply = Option(m.group(2)).getOrElse("last")
          s"""<div class="lpv--autostart" data-pgn="$esc" data-ply="$ply"></div>"""
        case _ => m.matched
  )

  // match the entire <a.../> tag with scheme & domain.  href value should be identical to inner text
  private val expandGameRegex =
    val quotedBaseUrl = Pattern.quote(baseUrl.value)
    val gameUrlRegex  = quotedBaseUrl + """/(\w{8})(?:/(white|black)|\w{4}|)(?:#(last|\d+))?"""
    ("<a href=\"" + gameUrlRegex + "\">" + gameUrlRegex + "</a>").r
  private val expandChapterRegex =
    val quotedBaseUrl   = Pattern.quote(baseUrl.value)
    val chapterUrlRegex = quotedBaseUrl + """/study/(?:embed/)?(?:\w{8})/(\w{8})(?:#(last|\d+))?"""
    ("<a href=\"" + chapterUrlRegex + "\">" + chapterUrlRegex + "</a>").r

  private val pgnCache = cacheApi.notLoadingSync[String, LpvEmbed](256, "prisblog.markup.pgn"):
    _.expireAfterWrite(10.seconds).build()

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

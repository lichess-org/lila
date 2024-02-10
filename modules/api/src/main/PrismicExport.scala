package lila.api

import io.prismic.{ Api as PrismicApi, * }
import io.prismic.DocumentLinkResolver
import io.prismic.Fragment.StructuredText
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.{ StandaloneWSClient, StandaloneWSResponse }
import play.api.mvc.MultipartFormData
import akka.stream.scaladsl.*
import akka.util.ByteString

import lila.common.config.MaxPerPage
import lila.blog.BlogPost
import lila.ublog.{ UblogPost, UblogBlog, UblogImage, UblogPostId }
import lila.memo.{ PicfitApi, PicfitUrl, PicfitImage }
import lila.user.User.lichessId
import lila.db.dsl.*
import lila.cms.CmsPage
import chess.variant.Variant

final private class PrismicExport(
    ublogApi: lila.ublog.UblogApi,
    blogApi: lila.blog.BlogApi,
    cmsApi: lila.cms.CmsApi,
    picfitApi: PicfitApi,
    picfitUrl: PicfitUrl,
    ws: StandaloneWSClient
)(using Executor, akka.stream.Materializer):

  private given StandaloneWSClient = ws

  def blogPosts(): Funit = for
    ctx     <- blogApi.context
    prismic <- blogApi.prismicApi
    _ <-
      blogApi
        .recentStream(ctx)
        .zipWithIndex
        .map: (p, i) =>
          logger.info(s"$i ${p.date.so(_.toString)} ${~p.title}")
          p
        .mapAsync(1)(convert(ctx.api))
        .runWith(Sink.ignore)
  yield ()

  def pages(): Funit = for
    _       <- variants()
    prismic <- blogApi.prismicApi
    bookmarks = prismic.bookmarks.toVector
    _ <- Source(bookmarks).zipWithIndex
      .map: (bm, i) =>
        logger.info(s"${i + 1}/${bookmarks.size} $bm")
        bm
      .mapAsync(1)(convertBookmark(prismic))
      .runWith(Sink.ignore)
      .void
  yield ()

  private def variants(): Funit = for
    prismic <- blogApi.prismicApi
    _ <- Source(Variant.list.all.filter(_.exotic)).zipWithIndex
      .map: (v, i) =>
        logger.info(s"${i + 1} ${v.name}")
        v
      .mapAsync(1)(convertVariant(prismic))
      .runWith(Sink.ignore)
      .void
  yield ()

  private def convertVariant(prismic: PrismicApi)(variant: Variant): Fu[Boolean] =
    val key      = CmsPage.Key(s"variant-${variant.key}")
    val language = lila.i18n.defaultLanguage
    val pageId   = CmsPage.Id.random
    val video = variant.key.value match
      case "racingKings" => "https://www.youtube.com/watch?v=GBUylnvSRYo\n"
      case "crazyhouse"  => "https://www.youtube.com/watch?v=VhZEc73HaYw\n"
      case "chess960"    => "https://www.youtube.com/watch?v=fUypgJQy7O8\n"
      case _             => ""
    cmsApi
      .get(key, language)
      .flatMap: page =>
        !page.isDefined so
          prismic
            .forms("variant")
            .query(s"""[[:d = at(my.variant.key, "${variant.key}")]]""")
            .ref(prismic.master.ref)
            .submit()
            .map(_.results.headOption)
            .flatMap:
              case None =>
                logger.error(s"Can't find prismic document for variant ${variant.key}")
                fuccess(true)
              case Some(doc) =>
                transferMarkdownImages(doc, pageId).flatMap: doc =>
                  cmsApi create CmsPage(
                    id = pageId,
                    key = key,
                    title = doc.getText("variant.title") | pageId.value,
                    markdown = htmlToMarkdown:
                      val html = ~doc.getHtml("variant.content", linkResolver(prismic))
                      lila.blog.BlogTransform.markdown:
                        Html(video + html)
                    ,
                    language = language,
                    live = true,
                    canonicalPath = s"/variant/${variant.key}".some,
                    by = lichessId,
                    at = nowInstant
                  ) inject true

  private val bookmarksPaths = Map(
    "tos"          -> "/terms-of-service",
    "how-to-cheat" -> "/how-to-cheat",
    "help"         -> "/help/contribute",
    "master"       -> "/help/master",
    "source"       -> "/source"
  )

  private def convertBookmark(prismic: PrismicApi)(bm: (String, String)): Fu[Boolean] =
    val key      = CmsPage.Key(bm._1)
    val language = lila.i18n.defaultLanguage
    val pageId   = CmsPage.Id.random
    cmsApi
      .get(key, language)
      .flatMap: page =>
        !page.isDefined so prismic
          .forms("everything")
          .query(s"""[[:d = at(document.id, "${bm._2}")]]""")
          .ref(prismic.master.ref)
          .submit()
          .dmap(_.results.headOption)
          .flatMap:
            case None =>
              logger.error(s"Can't find prismic document ${bm._2} for bookmark ${bm._1}")
              fuccess(true)
            case Some(doc) =>
              transferMarkdownImages(doc, pageId).flatMap: doc =>
                cmsApi create CmsPage(
                  id = pageId,
                  key = key,
                  title = doc.getText("doc.title") | pageId.value,
                  markdown = htmlToMarkdown:
                    lila.blog.BlogTransform.markdown:
                      Html(~doc.getHtml("doc.content", linkResolver(prismic)))
                  ,
                  language = language,
                  live = true,
                  canonicalPath = bookmarksPaths.get(key.value),
                  by = lichessId,
                  at = nowInstant
                ) inject true

  import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter
  private val htmlConverter: FlexmarkHtmlConverter = FlexmarkHtmlConverter.builder.build

  private def htmlToMarkdown(html: Html) = Markdown:
    htmlConverter
      .convert(html.value)
      .replace("""<br />""", "")
      .replaceAllIn("""<(https?://[^>]+)>""".r, "$1")

  private def uPostId(p: BlogPost) = UblogPostId(p.id take 8)

  private def uploadImage(fullUrl: String, rel: String): Fu[Option[PicfitImage]] =
    val url =
      if fullUrl.contains("images.prismic.io")
      then fullUrl.replaceAllIn("""^([^\?]+).*$""".r, "$1")
      else fullUrl
    ws.url(url).stream() flatMap:
      case res if res.status != 200 =>
        logger.error:
          s"Can't download image $url for $rel: ${res.status} ${res.statusText}"
        fuccess(none)
      case res =>
        val source: Source[ByteString, ?] = res.bodyAsSource
        val filename                      = url.split('/').last.takeWhile(_ != '?')
        val part = MultipartFormData.FilePart(
          key = "image",
          filename = filename,
          contentType = Option(if filename.endsWith(".png") then "image/png" else "image/jpeg"),
          ref = source
        )
        logger.debug(s"$rel $url > $filename")
        picfitApi.uploadSource(rel, part, lichessId).map(_.some)

  private def transferMainImage(post: BlogPost): Fu[Option[UblogImage]] =
    post
      .copy(imgSize = "main")
      .imageObj
      .so: img =>
        uploadImage(img.url, s"ublog:${uPostId(post)}") map2: pfi =>
          UblogImage(id = pfi.id, alt = img.alt, credit = none)

  private def transferMarkdownImages(doc: Document, pageId: CmsPage.Id): Fu[Document] =
    doc.get("doc.content") match
      case Some(StructuredText(blocks)) =>
        blocks
          .traverse:
            case i: StructuredText.Block.Image =>
              uploadImage(i.url, s"cmsPage:$pageId:${ornicar.scalalib.ThreadLocalRandom.nextString(12)}")
                .map:
                  _.fold(i): pfi =>
                    i.copy(view = i.view.copy(url = picfitUrl.resize(pfi.id, picfitApi.bodyImage.sizePx)))
            case b => fuccess(b)
          .map: blocks =>
            doc.copy(fragments = doc.fragments + ("doc.content" -> StructuredText(blocks)))
      case _ => fuccess(doc)

  private def transferMarkdownImages(post: BlogPost): Fu[BlogPost] = post.get("blog.body") match
    case Some(StructuredText(blocks)) =>
      blocks
        .traverse:
          case i: StructuredText.Block.Image =>
            uploadImage(i.url, s"ublog:${uPostId(post)}:${ornicar.scalalib.ThreadLocalRandom.nextString(12)}")
              .map:
                _.fold(i): pfi =>
                  i.copy(view = i.view.copy(url = picfitUrl.resize(pfi.id, picfitApi.bodyImage.sizePx)))
          case b => fuccess(b)
        .map: blocks =>
          post.copy(doc = post.doc.copy(fragments = post.fragments + ("blog.body" -> StructuredText(blocks))))
    case _ => fuccess(post)

  private def convert(prismic: PrismicApi)(p: BlogPost): Funit = for
    prev      <- ublogApi.getByPrismicId(p.id)
    _         <- prev.so(ublogApi.delete)
    mainImage <- transferMainImage(p)
    post      <- transferMarkdownImages(p)
    html = Html
      .from(post.getHtml("blog.body", linkResolver(prismic)))
      .map(lila.blog.Youtube.augmentEmbeds)
      .map(lila.blog.BlogTransform.removeProtocol)
      .map(lila.blog.BlogTransform.markdown.apply)
      .map(blogApi.expand)
    created = post.date match
      case Some(d) => d.atStartOfDay.instant
      case None =>
        logger.error(s"Blog post ${post.id} ${~post.title} has no date")
        nowInstant
    upost = UblogPost(
      id = uPostId(p),
      blog = UblogBlog.Id.User(lichessId),
      title = ~post.title,
      intro = post.shortlede,
      markdown = html.fold(Markdown(""))(htmlToMarkdown),
      language = lila.i18n.defaultLanguage,
      topics = Nil,
      image = mainImage,
      live = true,
      discuss = Option(false),
      created = UblogPost.Recorded(lichessId, created),
      updated = none,
      lived = UblogPost.Recorded(lichessId, created).some,
      likes = UblogPost.Likes(1),
      views = UblogPost.Views(0),
      rankAdjustDays = none,
      pinned = none
    )
    extra = $doc(
      "author"   -> post.author,
      "category" -> post.category,
      "forKids"  -> post.forKids
    )
    _ <- ublogApi.migrateFromBlog(upost, p.id, extra)
  yield ()

  private type MakeLinkResolver = PrismicApi => DocumentLinkResolver
  private given linkResolver: MakeLinkResolver = prismicApi =>
    DocumentLinkResolver(prismicApi): (link, _) =>
      s"/blog/${link.id}/${link.slug}"

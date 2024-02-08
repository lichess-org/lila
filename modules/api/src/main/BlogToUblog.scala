package lila.api

import io.prismic.{ Api as PrismicApi, * }
import io.prismic.DocumentLinkResolver
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.{ StandaloneWSClient, StandaloneWSResponse }

import lila.common.config.MaxPerPage
import lila.blog.BlogPost
import lila.user.User
import lila.ublog.UblogPost
import lila.ublog.UblogBlog
import lila.ublog.UblogImage
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.mvc.MultipartFormData
import lila.memo.PicfitApi
import lila.memo.PicfitImage
import lila.user.User.lichessId
import lila.memo.PicfitUrl
import io.prismic.Fragment.StructuredText

final private class BlogToUblog(
    ublogApi: lila.ublog.UblogApi,
    blogApi: lila.blog.BlogApi,
    picfitApi: PicfitApi,
    picfitUrl: PicfitUrl,
    ws: StandaloneWSClient
)(using Executor):

  private type MakeLinkResolver = PrismicApi => DocumentLinkResolver
  private given linkResolver: MakeLinkResolver = prismicApi =>
    DocumentLinkResolver(prismicApi): (link, _) =>
      s"/blog/${link.id}/${link.slug}"

  def all(): Funit = for
    ctx   <- blogApi.context
    pager <- blogApi.recent(ctx, 1, MaxPerPage(200))
    posts = pager.so(_.currentPageResults)
    prismic <- blogApi.prismicApi
    _       <- posts.traverse(convert(prismic))
  yield ()

  import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter
  private val htmlConverter: FlexmarkHtmlConverter = FlexmarkHtmlConverter.builder.build

  private def htmlToMarkdown(html: String) = Markdown:
    val raw = htmlConverter.convert(html)
    raw
      .replace("""<br />""", "")
      .replaceAllIn("""<(https?://[^>]+)>""".r, "$1")

  private def uPostId(p: BlogPost) = UblogPostId(p.id take 8)

  private def uploadImage(url: String, rel: String): Fu[Option[PicfitImage]] =
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
        logger.info(s"Uploading image $url as $filename for $rel")
        picfitApi.uploadSource(rel, part, lichessId).map(_.some)

  private def transferMainImage(post: BlogPost, upost: UblogPost): Fu[Option[UblogImage]] =
    post.imageObj.so: img =>
      uploadImage(img.url, s"ublog:${upost.id}") map2: pfi =>
        UblogImage(id = pfi.id, alt = img.alt, credit = none)

  private def transferMarkdownImages(post: BlogPost): Fu[BlogPost] = post.get("blog.body") match
    case Some(StructuredText(blocks)) =>
      blocks
        .traverse:
          case i: StructuredText.Block.Image =>
            uploadImage(i.url, s"ublog:${uPostId(post)}:${ornicar.scalalib.ThreadLocalRandom.nextString(12)}")
              .map:
                _.fold(i): pfi =>
                  i.copy(view = i.view.copy(url = UblogPost.thumbnail(picfitUrl, pfi.id, _.Size.Large)))
          case b => fuccess(b)
        .map: blocks =>
          post.copy(doc = post.doc.copy(fragments = post.fragments + ("blog.body" -> StructuredText(blocks))))
    case _ => fuccess(post)

  private def convert(prismic: PrismicApi)(p: BlogPost): Funit = for
    post <- transferMarkdownImages(p)
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
      markdown = htmlToMarkdown(html.so(_.value)),
      language = lila.i18n.defaultLanguage,
      topics = Nil,
      image = none,
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
    mainImage <- transferMainImage(p, upost)
    newPost = upost.copy(image = mainImage)
    _ <- ublogApi.migrateFromBlog(newPost, p.id)
  yield ()

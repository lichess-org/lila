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

final private class BlogToUblog(
    ublogApi: lila.ublog.UblogApi,
    blogApi: lila.blog.BlogApi,
    picfitApi: PicfitApi,
    ws: StandaloneWSClient
)(using Executor):

  private type MakeLinkResolver = (PrismicApi, Option[String]) => DocumentLinkResolver
  private given linkResolver: MakeLinkResolver = (prismicApi, ref) =>
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

  private def transferImage(post: BlogPost, upost: UblogPost): Fu[Option[UblogImage]] =
    post.imageObj.so: img =>
      ws.url(img.url).stream() flatMap:
        case res if res.status != 200 =>
          logger.error:
            s"Can't download blog ${upost.title} image ${img.url}: ${res.status} ${res.statusText}"
          fuccess(none)
        case res =>
          val source: Source[ByteString, ?] = res.bodyAsSource
          val filename                      = img.url.split('/').last.takeWhile(_ != '?')
          val part = MultipartFormData.FilePart(
            key = "image",
            // https://images.prismic.io/lichess/0a96f6a8-1913-4320-8dbd-addda383ba8f_lichessupdate.png?auto=compress,format
            filename = filename,
            contentType = Option(if filename.endsWith(".png") then "image/png" else "image/jpeg"),
            ref = source
          )
          picfitApi
            .uploadSource(s"ublog:${upost.id}", part, upost.created.by)
            .map: picfitImage =>
              UblogImage(
                id = picfitImage.id,
                alt = img.alt,
                credit = none
              ).some

  private def convert(prismic: PrismicApi)(p: BlogPost): Funit =
    import p.*
    val userId = User.lichessId
    val html = Html
      .from(getHtml("blog.body", linkResolver(prismic, none)))
      .map(lila.blog.Youtube.augmentEmbeds)
      .map(lila.blog.BlogTransform.removeProtocol)
      .map(lila.blog.BlogTransform.markdown.apply)
      .map(blogApi.expand)
    val created = date match
      case Some(d) => d.atStartOfDay.instant
      case None =>
        logger.error(s"Blog post $id ${~title} has no date")
        nowInstant
    val upost = UblogPost(
      id = UblogPostId(p.id take 8),
      blog = UblogBlog.Id.User(userId),
      title = ~title,
      intro = shortlede,
      markdown = htmlToMarkdown(html.so(_.value)),
      language = lila.i18n.defaultLanguage,
      topics = Nil,
      image = none,
      live = true,
      discuss = Option(false),
      created = UblogPost.Recorded(userId, created),
      updated = none,
      lived = UblogPost.Recorded(userId, created).some,
      likes = UblogPost.Likes(1),
      views = UblogPost.Views(0),
      rankAdjustDays = none,
      pinned = none
    )
    for
      image <- transferImage(p, upost)
      _     <- ublogApi.migrateFromBlog(upost.copy(image = image), p.id)
    yield ()

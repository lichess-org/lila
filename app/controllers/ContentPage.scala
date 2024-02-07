package controllers

import play.api.libs.json.*
import chess.variant.Variant
import lila.app.{ given, * }

final class ContentPage(
    env: Env,
    prismicC: Prismic
) extends LilaController(env):

  val help   = menuBookmark("help")
  val tos    = menuBookmark("tos")
  val master = menuBookmark("master")

  def bookmark(name: String, active: Option[String])(using Context) =
    FoundPage(prismicC getBookmark name): p =>
      active match
        case None       => views.html.site.page.lone(p)
        case Some(name) => views.html.site.page.withMenu(name, p)

  def loneBookmark(name: String) = Open:
    Found(prismicC getBookmark name): p =>
      (for
        page <- p.left.toOption
        path <- page.canonicalPath
        if req.path == s"/page/$name"
        if req.path != path
      yield path) match
        case Some(path) => Redirect(path)
        case None =>
          pageHit
          Ok.pageAsync(views.html.site.page.lone(p))

  def menuBookmark(name: String) = Open:
    pageHit
    FoundPage(prismicC getBookmark name):
      views.html.site.page.withMenu(name, _)

  def source = Open:
    pageHit
    FoundPage(prismicC getBookmark "source"):
      views.html.site.page.source

  def variantHome = Open:
    negotiate(
      Ok.pageAsync(views.html.site.variant.home),
      Ok(lila.api.StaticContent.variantsJson)
    )

  def variant(key: Variant.LilaKey) = Open:
    (for
      variant  <- Variant(key)
      perfType <- lila.rating.PerfType byVariant variant
    yield FoundPage(prismicC getVariant variant): p =>
      views.html.site.variant.show(p, variant, perfType)) | notFound

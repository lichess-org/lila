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

  def bookmark(name: String, active: Option[String]) = Open:
    pageHit
    FoundPage(prismicC getBookmark name): p =>
      active match
        case None       => views.html.site.page.lone(p)
        case Some(name) => views.html.site.page.withMenu(name, p)

  def loneBookmark(name: String) = bookmark(name, none)
  def menuBookmark(name: String) = bookmark(name, name.some)

  def source = Open:
    pageHit
    FoundPage(prismicC getBookmark "source"):
      views.html.site.page.source

  def variantHome = Open:
    negotiate(
      FoundPage(prismicC getBookmark "variant"):
        views.html.site.variant.home
      ,
      Ok(lila.api.StaticContent.variantsJson)
    )

  def variant(key: Variant.LilaKey) = Open:
    (for
      variant  <- Variant(key)
      perfType <- lila.rating.PerfType byVariant variant
    yield FoundPage(prismicC getVariant variant): (doc, resolver) =>
      views.html.site.variant.show(doc, resolver, variant, perfType)) | notFound

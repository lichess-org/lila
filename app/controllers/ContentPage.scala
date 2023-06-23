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
    FoundPage(prismicC getBookmark name): (doc, resolver) =>
      active match
        case None       => views.html.site.page.lone(doc, resolver)
        case Some(name) => views.html.site.page.withMenu(name, doc, resolver)

  def loneBookmark(name: String) = bookmark(name, none)
  def menuBookmark(name: String) = bookmark(name, name.some)

  def source = Open:
    pageHit
    FoundPage(prismicC getBookmark "source"): (doc, resolver) =>
      views.html.site.page.source(doc, resolver)

  def variantHome = Open:
    negotiate(
      FoundPage(prismicC getBookmark "variant"): (doc, resolver) =>
        views.html.site.variant.home(doc, resolver),
      Ok(lila.api.StaticContent.variantsJson)
    )

  def variant(key: Variant.LilaKey) = Open:
    (for
      variant  <- Variant(key)
      perfType <- lila.rating.PerfType byVariant variant
    yield FoundPage(prismicC getVariant variant): (doc, resolver) =>
      views.html.site.variant.show(doc, resolver, variant, perfType)) | notFound

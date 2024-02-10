package controllers

import play.api.libs.json.*
import chess.variant.Variant
import lila.app.{ given, * }
import lila.cms.CmsPage

final class ContentPage(env: Env) extends LilaController(env):

  val help   = menuBookmark(CmsPage.Key("help"))
  val tos    = menuBookmark(CmsPage.Key("tos"))
  val master = menuBookmark(CmsPage.Key("master"))

  def bookmark(key: CmsPage.Key, active: Option[String])(using Context) =
    FoundPage(env.api.cmsRender(key)): p =>
      active match
        case None       => views.html.site.page.lone(p)
        case Some(name) => views.html.site.page.withMenu(name, p)

  def loneBookmark(key: CmsPage.Key) = Open:
    Found(env.api.cmsRender(key)): p =>
      p.canonicalPath.filter(_ != req.path && req.path == s"/page/$key") match
        case Some(path) => Redirect(path)
        case None =>
          pageHit
          Ok.pageAsync(views.html.site.page.lone(p))

  def menuBookmark(key: CmsPage.Key) = Open:
    pageHit
    FoundPage(env.api cmsRender key):
      views.html.site.page.withMenu(key.value, _)

  def source = Open:
    pageHit
    FoundPage(env.api cmsRenderKey "source"):
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
    yield FoundPage(env.api.cmsRenderKey(s"variant-${variant.key}")): p =>
      views.html.site.variant.show(p, variant, perfType)) | notFound

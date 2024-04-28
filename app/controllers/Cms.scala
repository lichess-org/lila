package controllers

import play.api.mvc.*

import lila.app.*
import lila.cms.CmsPage

final class Cms(env: Env) extends LilaController(env):

  def api = env.cms.api

  // crud

  def index = Secure(_.Pages): ctx ?=>
    for
      pages        <- api.list
      renderedPage <- renderPage(views.cms.index(pages))
    yield Ok(renderedPage)

  def createForm = Secure(_.Pages) { _ ?=> _ ?=>
    Ok.pageAsync(views.cms.create(env.cms.form.create))
  }

  def create = SecureBody(_.Pages) { _ ?=> me ?=>
    env.cms.form.create
      .bindFromRequest()
      .fold(
        err => BadRequest.pageAsync(views.cms.create(err)),
        data =>
          val page = data.create(me)
          api.create(page).inject(Redirect(routes.Cms.edit(page.id)).flashSuccess)
      )
  }

  def edit(id: CmsPage.Id) = Secure(_.Pages) { _ ?=> _ ?=>
    Found(api.withAlternatives(id)): pages =>
      Ok.pageAsync(views.cms.edit(env.cms.form.edit(pages.head), pages.head, pages.tail))
  }

  def update(id: CmsPage.Id) = SecureBody(_.Pages) { _ ?=> me ?=>
    Found(api.withAlternatives(id)): pages =>
      env.cms.form
        .edit(pages.head)
        .bindFromRequest()
        .fold(
          err => BadRequest.pageAsync(views.cms.edit(err, pages.head, pages.tail)),
          data =>
            api
              .update(pages.head, data)
              .map: page =>
                Redirect(routes.Cms.edit(page.id)).flashSuccess
        )
  }

  def delete(id: CmsPage.Id) = Secure(_.Pages) { _ ?=> _ ?=>
    Found(api.get(id)): up =>
      api.delete(up.id).inject(Redirect(routes.Cms.index).flashSuccess)
  }

  // pages

  val help   = menuPage(CmsPage.Key("help"))
  val tos    = menuPage(CmsPage.Key("tos"))
  val master = menuPage(CmsPage.Key("master"))

  def page(key: CmsPage.Key, active: Option[String])(using Context) =
    FoundPage(env.api.cmsRender(key)): p =>
      active match
        case None       => views.site.page.lone(p)
        case Some(name) => views.site.page.withMenu(name, p)

  def lonePage(key: CmsPage.Key) = Open:
    Found(env.api.cmsRender(key)): p =>
      p.canonicalPath.filter(_ != req.path && req.path == s"/page/$key") match
        case Some(path) => Redirect(path)
        case None =>
          pageHit
          Ok.pageAsync(views.site.page.lone(p))

  def menuPage(key: CmsPage.Key) = Open:
    pageHit
    FoundPage(env.api.cmsRender(key)):
      views.site.page.withMenu(key.value, _)

  def source = Open:
    pageHit
    FoundPage(env.api.cmsRenderKey("source")):
      views.site.page.source

  def variantHome = Open:
    negotiate(
      Ok.pageAsync(views.site.variant.home),
      Ok(lila.web.StaticContent.variantsJson)
    )

  import chess.variant.Variant
  def variant(key: Variant.LilaKey) = Open:
    (for
      variant <- Variant(key)
      perfKey <- PerfKey.byVariant(variant)
    yield FoundPage(env.api.cmsRenderKey(s"variant-${variant.key}")): p =>
      views.site.variant.show(p, variant, perfKey)) | notFound

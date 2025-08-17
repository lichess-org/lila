package controllers

import play.api.mvc.*

import lila.app.*
import lila.cms.CmsPage
import lila.core.id.{ CmsPageId, CmsPageKey }

final class Cms(env: Env) extends LilaController(env):

  def api = env.cms.api

  // crud

  def index = Secure(_.Pages): ctx ?=>
    for
      pages <- api.list
      renderedPage <- renderPage(views.cms.index(pages))
    yield Ok(renderedPage)

  def createForm(key: Option[CmsPageKey]) = Secure(_.Pages) { _ ?=> _ ?=>
    Ok.async(views.cms.create(env.cms.form.create, key))
  }

  def create = SecureBody(_.Pages) { _ ?=> me ?=>
    bindForm(env.cms.form.create)(
      err => BadRequest.async(views.cms.create(err, none)),
      data =>
        val page = data.create(me)
        api.create(page).inject(Redirect(routes.Cms.edit(page.id)).flashSuccess)
    )
  }

  def edit(id: CmsPageId) = Secure(_.Pages) { _ ?=> _ ?=>
    Found(api.withAlternatives(id)): pages =>
      Ok.async(views.cms.edit(env.cms.form.edit(pages.head), pages.head, pages.tail))
  }

  def update(id: CmsPageId) = SecureBody(_.Pages) { _ ?=> me ?=>
    Found(api.withAlternatives(id)): pages =>
      bindForm(env.cms.form.edit(pages.head))(
        err => BadRequest.async(views.cms.edit(err, pages.head, pages.tail)),
        data =>
          api
            .update(pages.head, data)
            .map: page =>
              Redirect(routes.Cms.edit(page.id)).flashSuccess
      )
  }

  def delete(id: CmsPageId) = Secure(_.Pages) { _ ?=> _ ?=>
    Found(api.get(id)): up =>
      api.delete(up.id).inject(Redirect(routes.Cms.index).flashSuccess)
  }

  // pages

  val help = menuPage(CmsPageKey("help"))
  val tos = menuPage(CmsPageKey("tos"))

  def page(key: CmsPageKey, active: Option[String])(using Context) =
    FoundPage(env.cms.render(key)): p =>
      active.fold(views.cms.lone(p)):
        views.site.page.withMenu(_, p)

  def lonePage(key: CmsPageKey) = Open:
    orCreateOrNotFound(key): page =>
      page.canonicalPath.filter(_ != req.path && req.path == s"/page/$key") match
        case Some(path) => Redirect(path)
        case None =>
          pageHit
          Ok.async(views.cms.lone(page))

  def orCreateOrNotFound(key: CmsPageKey)(f: CmsPage.Render => Fu[Result])(using Context): Fu[Result] =
    env.cms
      .render(key)
      .flatMap:
        case Some(page) => f(page)
        case None =>
          import lila.ui.Context.ctxMe // no idea why this is needed here
          if isGrantedOpt(_.Pages)
          then Ok.async(views.cms.create(env.cms.form.create, key.some))
          else notFound

  def menuPage(key: CmsPageKey) = Open:
    pageHit
    FoundPage(env.cms.render(key)):
      views.site.page.withMenu(key.value, _)

  def source = Open:
    pageHit
    FoundPage(env.cms.renderKey("source")): p =>
      views.site.ui.source(p.title, views.cms.render(p), env.web.lilaVersion)

  def variantHome = Open:
    negotiate(
      Ok.async(views.site.variant.home),
      Ok(lila.web.StaticContent.variantsJson)
    )

  import chess.variant.Variant
  def variant(key: Variant.LilaKey) = Open:
    (for
      variant <- Variant(key)
      perfKey <- PerfKey.byVariant(variant)
    yield FoundPage(env.cms.renderKey(s"variant-${variant.key}")): p =>
      views.site.variant.show(p, variant, perfKey)) | notFound

package controllers

import play.api.mvc.*

import lila.app.*
import lila.cms.CmsPage
import lila.core.id.{ CmsPageId, CmsPageKey }
import lila.common.HTTPRequest

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
    negotiateCms(key): page =>
      active.fold(views.cms.lone(page)):
        views.site.page.withMenu(_, page)

  def lonePage(key: CmsPageKey) = Open:
    orCreateOrNotFound(key): page =>
      page.canonicalPath.filter(_ != req.path && req.path == s"/page/$key") match
        case Some(path) => Redirect(path)
        case None =>
          pageHit
          Ok.async(views.cms.lone(page))

  def orCreateOrNotFound(key: CmsPageKey)(f: CmsPage.Render => Fu[Result])(using Context): Fu[Result] =
    negotiateCmsOption(key).getOrElse:
      for
        found <- env.cms.render(key)
        res <- found match
          case Some(page) => f(page)
          case None =>
            import lila.ui.Context.ctxMe // no idea why this is needed here
            if isGrantedOpt(_.Pages)
            then Ok.async(views.cms.create(env.cms.form.create, key.some))
            else notFound
      yield res

  def menuPage(key: CmsPageKey) = Open:
    pageHit
    negotiateCms(key): page =>
      views.site.page.withMenu(key.value, page)

  def source = Open:
    pageHit
    negotiateCms(CmsPageKey("source")): page =>
      views.site.ui.source(page.title, views.cms.render(page), env.web.lilaVersion)

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
    yield negotiateCms(CmsPageKey(s"variant-${variant.key}")): page =>
      views.site.variant.show(page, variant, perfKey)) | notFound

  private def negotiateCms(
      key: CmsPageKey
  )(f: CmsPage.Render => Fu[lila.ui.Page])(using Context): Fu[Result] =
    negotiateCmsOption(key).getOrElse:
      FoundPage(env.cms.render(key))(f)

  private def negotiateCmsOption(key: CmsPageKey)(using Context): Option[Fu[Result]] =
    HTTPRequest.acceptsMarkdown.option:
      for text <- env.cms.api.asMarkdown(key)
      yield text.fold(notFoundText()): text =>
        Ok(text).withHeaders(CONTENT_TYPE -> "text/markdown; charset=utf-8")

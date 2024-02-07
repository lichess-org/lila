package views.html

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.cms.CmsPage
import play.api.data.Form
import lila.common.String.shorten

object cms:

  import controllers.Prismic.*

  def render(p: AnyPage)(using Context) = frag(
    editButton(p),
    rawHtml(p.html)
  )

  def editButton(p: AnyPage)(using Context) =
    isGranted(_.Pages) option a(
      href     := p.pageId.fold(routes.Cms.create)(routes.Cms.edit(_)),
      cls      := "button button-empty text",
      dataIcon := licon.Pencil
    )("Edit")

  private def layout(title: String, edit: Boolean = false)(body: Frag)(using PageContext) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("cms"),
      moreJs = jsModule("cms")
    ):
      main(cls := "page-menu")(mod.menu("cms"), div(cls := "page-menu__content cms box box-pad")(body))

  def index(pages: List[CmsPage])(using PageContext) =
    layout("Lichess pages"):
      frag(
        boxTop(
          h1("Lichess pages"),
          div(cls := "box__top__actions"):
            a(
              href     := routes.Cms.createForm,
              cls      := "button button-green",
              dataIcon := licon.PlusButton
            )
        ),
        standardFlash,
        table(cls := "cms__pages slist")(
          thead(
            tr(
              th("Page"),
              th("Content"),
              th("Live"),
              th("Errors"),
              th(dataSortDefault)("Updated")
            )
          ),
          tbody(
            pages
              .map: page =>
                tr(
                  td(dataSort := page.id)(a(href := routes.Cms.edit(page.id))(page.title), br, code(page.id)),
                  td(shorten(page.markdown.value, 140)),
                  td(
                    if page.live then goodTag(iconTag(licon.Checkmark))
                    else badTag(iconTag(licon.X))
                  ),
                  td(dataSort := ~page.error)(page.error.fold(goodTag(iconTag(licon.Checkmark)))(badTag(_))),
                  td(dataSort := page.at.toMillis)(
                    userIdLink(page.by.some, withOnline = false, withTitle = false),
                    br,
                    momentFromNow(page.at)
                  )
                )
          )
        )
      )

  def create(form: Form[?])(using PageContext) =
    layout("Lichess pages: New", true):
      frag(
        boxTop(h1(a(href := routes.Cms.index)("Lichess pages"), " • ", "New page!")),
        postForm(cls := "content_box_content form3", action := routes.Cms.create):
          inForm(form, none)
      )

  def edit(form: Form[?], page: CmsPage)(using PageContext) =
    layout(s"Lichess page ${page.id}", true):
      frag(
        boxTop(
          h1(a(href := routes.Cms.index)("Lichess page"), " • ", page.id),
          div(cls := "box__top__actions"):
            a(
              href     := page.canonicalPath.getOrElse(routes.ContentPage.loneBookmark(page.id)),
              cls      := "button button-green",
              dataIcon := licon.Eye
            )
        ),
        standardFlash,
        postForm(cls := "content_box_content form3", action := routes.Cms.update(page.id)):
          inForm(form, page.some)
        ,
        postForm(action := routes.Cms.delete(page.id))(cls := "cms__delete"):
          submitButton(cls := "button button-red button-empty confirm")("Delete")
      )

  private def inForm(form: Form[?], page: Option[CmsPage])(using Context) =
    frag(
      form3.split(
        form3.group(
          form("title"),
          "Title",
          half = true,
          help = frag("The title is prepended to the page content, so no need to repeat it there.").some
        )(form3.input(_)(autofocus)),
        form3.group(
          form("id"),
          "ID",
          half = true,
          help = frag(
            "Used as part of the page URL: /page/{ID}. Sometimes also used by lila to display somewhere else. Be very careful when changing it."
          ).some
        )(form3.input(_))
      ),
      form3.group(
        form("markdown"),
        frag("Content", page.flatMap(_.error).map(err => frag(br, badTag(err)))),
        help = trans.embedsAvailable().some
      ): field =>
        frag(
          form3.textarea(field)(),
          div(cls := "markdown-editor", attr("data-image-upload-url") := routes.Main.uploadImage("cmsPage"))
        ),
      form3.split(
        form3.group(form("language"), trans.language(), half = true, help = raw("Not used yet.").some):
          form3.select(_, lila.i18n.LangForm.popularLanguages.choices)
        ,
        form3.checkbox(form("live"), raw("Live"), half = true)
      ),
      form3.split(
        form3.group(
          form("canonicalPath"),
          "Canonical path",
          half = true,
          help =
            frag("The URL of the dedicated page of this content, if any. Example: /variant/crazyhouse").some
        )(form3.input(_)(autofocus))
      ),
      form3.action(form3.submit("Save"))
    )

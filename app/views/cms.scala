package views.html

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.cms.CmsPage
import play.api.data.Form

object cms:

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
              th("Title"),
              th("Content"),
              th("Updated")
            )
          ),
          tbody(
            pages
              .map: page =>
                tr(
                  td(a(href := routes.Cms.edit(page.id))(page.title), br, code(page.id)),
                  td(page.markdown.value.take(100)),
                  td(userIdLink(page.by.some), br, momentFromNow(page.at))
                )
          )
        )
      )

  def create(form: Form[?])(using PageContext) =
    layout("Lichess pages: New", true):
      frag(
        boxTop(h1(a(href := routes.Cms.index)("Lichess pages"), " • ", "New page!")),
        postForm(cls := "content_box_content form3", action := routes.Cms.create):
          inForm(form)
      )

  def edit(form: Form[?], page: CmsPage)(using PageContext) =
    layout(s"Lichess page ${page.id}", true):
      frag(
        boxTop(
          h1(a(href := routes.Cms.index)("Lichess page"), " • ", page.id)
        ),
        standardFlash,
        postForm(cls := "content_box_content form3", action := routes.Cms.update(page.id)):
          inForm(form)
        ,
        postForm(action := routes.Cms.delete(page.id))(cls := "cms__delete"):
          submitButton(cls := "button button-red button-empty confirm")("Delete")
      )

  private def inForm(form: Form[?])(using Context) =
    frag(
      form3.split(
        form3.group(form("title"), "Title", half = true)(form3.input(_)(autofocus)),
        form3.group(form("id"), "ID", half = true)(form3.input(_))
      ),
      form3.group(form("markdown"), "Content", help = trans.embedsAvailable().some): field =>
        frag(
          form3.textarea(field)(),
          div(cls := "markdown-editor", attr("data-image-upload-url") := routes.Main.uploadImage("cmsPage"))
        ),
      form3.split(
        form3.group(form("language"), trans.language(), half = true):
          form3.select(_, lila.i18n.LangForm.popularLanguages.choices)
        ,
        form3.checkbox(form("live"), raw("Live"), half = true)
      ),
      form3.action(form3.submit("Save"))
    )

package lila.cms
package ui

import play.api.data.Form
import play.api.libs.json.*

import lila.ui.*
import ScalatagsTemplate.{ *, given }

final class CmsUi(helpers: Helpers)(menu: Context ?=> Frag):
  import helpers.{ *, given }

  private given cmsPageIdConv: Conversion[CmsPage.Id, String]   = _.value
  private given cmsPageKeyConv: Conversion[CmsPage.Key, String] = _.value

  def render(page: CmsPage.Render)(using Context) =
    if !page.live && !Granter.opt(_.Pages)
    then p("Oops, looks like there will be something here soon... but not yet!")
    else
      frag(
        editButton(page),
        (!page.live).option(
          span(cls := "cms__draft text", dataIcon := Icon.Eye)(
            "This draft is not published"
          )
        ),
        rawHtml(page.html)
      )

  private def editButton(p: CmsPage.Render)(using Context) =
    Granter
      .opt(_.Pages)
      .option(
        a(
          href     := routes.Cms.edit(p.id),
          cls      := "button button-empty text",
          dataIcon := Icon.Pencil
        )("Edit")
      )

  private def layout(title: String)(mods: AttrPair*)(using Context) =
    Page(title)
      .cssTag("cms")
      .js(EsmInit("bits.cms"))
      .wrap: body =>
        main(cls := "page-menu")(menu, div(cls := "page-menu__content cms box")(mods)(body))

  def index(pages: List[CmsPage])(using Context) =
    layout("Lichess pages")():
      frag(
        boxTop(
          h1("Lichess pages"),
          div(cls := "box__top__actions")(
            input(cls := "cms__pages__search", placeholder := trans.search.search.txt(), autofocus),
            a(
              href     := routes.Cms.createForm,
              cls      := "button button-green",
              dataIcon := Icon.PlusButton
            )
          )
        ),
        standardFlash,
        renderTable(pages)
      )

  private def renderTable(pages: List[CmsPage], tableName: String = "Page")(using Context) =
    table(cls := "cms__pages slist slist-pad")(
      thead(
        tr(
          th(tableName),
          th("Content"),
          th("Lang"),
          th("Live"),
          th(dataSortDefault)("Updated")
        )
      ),
      tbody(
        pages
          .map: page =>
            tr(
              td(dataSort := page.key, cls := "title")(
                a(href := routes.Cms.edit(page.id))(page.title),
                br,
                code(page.key)
              ),
              td(shorten(page.markdown.value, 140)),
              td(cls := "lang")(page.language.toUpperCase),
              td(
                if page.live then goodTag(iconTag(Icon.Checkmark))
                else badTag(iconTag(Icon.X))
              ),
              td(dataSort := page.at.toMillis)(
                userIdLink(page.by.some, withOnline = false, withTitle = false),
                br,
                momentFromNow(page.at)
              )
            )
      )
    )

  def create(form: Form[?])(using Context) =
    layout("Lichess pages: New")(cls := "box-pad"):
      frag(
        boxTop(h1(a(href := routes.Cms.index)("Lichess pages"), " • ", "New page!")),
        postForm(cls := "content_box_content form3", action := routes.Cms.create):
          inForm(form)
      )

  def edit(form: Form[?], page: CmsPage, alts: List[CmsPage])(using Context) =
    layout(s"Lichess page ${page.key}")(cls := "box-pad"):
      frag(
        boxTop(
          h1(a(href := routes.Cms.index)("Lichess page"), " • ", page.key),
          div(cls := "box__top__actions"):
            a(
              href     := page.canonicalPath.getOrElse(routes.Cms.lonePage(page.key).url),
              cls      := "button button-green",
              dataIcon := Icon.Eye
            )
        ),
        standardFlash,
        alts.nonEmpty.option(
          div(cls := "cms__alternatives")(
            renderTable(alts, "Alt languages"),
            br,
            br
          )
        ),
        postForm(cls := "content_box_content form3", action := routes.Cms.update(page.id)):
          inForm(form)
        ,
        postForm(action := routes.Cms.delete(page.id))(cls := "cms__delete"):
          submitButton(cls := "button button-red button-empty confirm")("Delete")
      )

  private def inForm(form: Form[?])(using Context) =
    frag(
      form3.split(
        form3.group(
          form("title"),
          "Title",
          half = true,
          help = frag("The title is prepended to the page content, so no need to repeat it there.").some
        )(form3.input(_)(autofocus)),
        form3.group(
          form("key"),
          "Key",
          half = true,
          help = frag(
            "Used as part of the page URL: /page/{ID}. Sometimes also used by lila to display somewhere else. Be very careful when changing it."
          ).some
        )(form3.input(_))
      ),
      form3.split(
        form3.group(
          form("language"),
          trans.site.language(),
          half = true,
          help = raw("Language of this content. Helps selecting the right content for each viewer.").some
        ):
          form3.select(_, langList.popularLanguagesForm.choices)
        ,
        form3.group(
          form("canonicalPath"),
          "Canonical path",
          half = true,
          help =
            frag("The URL of the dedicated page of this content, if any. Example: /variant/crazyhouse").some
        )(form3.input(_))
      ),
      form3.group(
        form("markdown"),
        frag("Content"),
        help = trans.site.embedsAvailable().some
      ): field =>
        frag(
          form3.textarea(field)(),
          div(cls := "markdown-editor", attr("data-image-upload-url") := routes.Main.uploadImage("cmsPage"))
        ),
      form3.split(
        form3.checkbox(form("live"), raw("Live"), half = true)
      ),
      form3.action(form3.submit("Save"))
    )

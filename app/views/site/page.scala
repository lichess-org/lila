package views.site

import lila.app.UiEnv.{ *, given }
import lila.cms.CmsPage

lazy val ui = lila.web.ui.SitePages(helpers)

object page:

  export ui.SitePage

  def lone(p: CmsPage.Render)(using ctx: Context) =
    Page(p.title)
      .cssTag("page")
      .js((p.key == CmsPage.Key("fair-play")).option(embedJsUnsafeLoadThen("""$('.slist td').each(function() {
if (this.innerText == 'YES') this.style.color = 'green'; else if (this.innerText == 'NO') this.style.color = 'red';
})"""))):
        main(cls := "page-small box box-pad page force-ltr")(pageContent(p))

  def withMenu(active: String, p: CmsPage.Render)(using Context) =
    SitePage(
      title = p.title,
      active = active,
      contentCls = "page box box-pad force-ltr"
    ).cssTag("page")(pageContent(p))

  def pageContent(p: CmsPage.Render)(using Context) = frag(
    h1(cls := "box__top")(p.title),
    div(cls := "body")(views.cms.render(p))
  )

  lazy val faq = lila.web.ui.FaqUi(helpers, ui)(
    standardRankableDeviation = lila.rating.Glicko.standardRankableDeviation,
    variantRankableDeviation = lila.rating.Glicko.variantRankableDeviation
  )

  def contact(using Context) =
    SitePage(
      title = trans.contact.contact.txt(),
      active = "contact",
      contentCls = "page box box-pad"
    ).cssTag("contact").js(EsmInit("bits.contact"))(lila.web.ui.contact(netConfig.email))

  def source(p: CmsPage.Render)(using ctx: Context) =
    ui.source(
      p.title,
      views.cms.render(p),
      env.appVersionCommit | "???",
      env.appVersionDate,
      env.appVersionMessage
    )

  def webmasters(using Context) =
    ui.webmasters(
      li(strong("theme"), ": ", lila.pref.Theme.all.map(_.name).mkString(", ")),
      li(strong("pieceSet"), ": ", lila.pref.PieceSet.all.map(_.name).mkString(", "))
    )

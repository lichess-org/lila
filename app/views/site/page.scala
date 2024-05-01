package views.site

import lila.app.templating.Environment.{ *, given }
import lila.cms.CmsPage

lazy val ui = lila.web.views.SitePages(helpers)

object page:

  export ui.SitePage

  def lone(p: CmsPage.Render)(using ctx: PageContext) =
    Page(p.title)
      .cssTag("page")
      .js((p.key == CmsPage.Key("fair-play")).option(embedJsUnsafeLoadThen("""$('.slist td').each(function() {
if (this.innerText == 'YES') this.style.color = 'green'; else if (this.innerText == 'NO') this.style.color = 'red';
})"""))):
        main(cls := "page-small box box-pad page force-ltr")(pageContent(p))

  def withMenu(active: String, p: CmsPage.Render)(using PageContext) =
    SitePage(
      title = p.title,
      active = active,
      contentCls = "page box box-pad force-ltr"
    ).cssTag("page")(pageContent(p))

  def pageContent(p: CmsPage.Render)(using Context) = frag(
    h1(cls := "box__top")(p.title),
    div(cls := "body")(views.cms.render(p))
  )

  def faq(using PageContext) =
    SitePage(
      title = "Frequently Asked Questions",
      active = "faq"
    ).cssTag("faq"):
      lila.web.views.faq(helpers, assetHelper)(
        standardRankableDeviation = lila.rating.Glicko.standardRankableDeviation,
        variantRankableDeviation = lila.rating.Glicko.variantRankableDeviation
      )

  def contact(using PageContext) =
    SitePage(
      title = trans.contact.contact.txt(),
      active = "contact",
      contentCls = "page box box-pad"
    ).cssTag("contact").js(EsmInit("bits.contact"))(lila.web.views.contact(netConfig.email))

  def source(p: CmsPage.Render)(using ctx: PageContext) =
    ui.source(
      p.title,
      views.cms.render(p),
      env.appVersionCommit | "???",
      env.appVersionDate,
      env.appVersionMessage
    )

  def webmasters(using PageContext) =
    ui.webmasters(
      li(strong("theme"), ": ", lila.pref.Theme.all.map(_.name).mkString(", ")),
      li(strong("pieceSet"), ": ", lila.pref.PieceSet.all.map(_.name).mkString(", "))
    )

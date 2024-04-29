package views.site

import lila.app.templating.Environment.{ *, given }

import lila.cms.CmsPage

object page:

  def lone(p: CmsPage.Render)(using ctx: PageContext) =
    views.base.layout(
      moreCss = cssTag("page"),
      title = p.title,
      moreJs = (p.key == CmsPage.Key("fair-play")).option(embedJsUnsafeLoadThen("""$('.slist td').each(function() {
if (this.innerText == 'YES') this.style.color = 'green'; else if (this.innerText == 'NO') this.style.color = 'red';
})""")(ctx.nonce))
    ):
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

  private lazy val ui = lila.web.views.SitePages(helpers)

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
    val commit = env.appVersionCommit | "???"
    SitePage(title = p.title, active = "source", contentCls = "page force-ltr")
      .cssTag("source")
      .js(
        embedJsUnsafeLoadThen:
          """$('#asset-version-date').text(site.info.date);
  $('#asset-version-commit').attr('href', 'https://github.com/lichess-org/lila/commits/' + site.info.commit).find('pre').text(site.info.commit.substr(0, 7));
  $('#asset-version-upcoming').attr('href', 'https://github.com/lichess-org/lila/compare/' + site.info.commit + '...master').find('pre').text('...');
  $('#asset-version-message').text(site.info.message);"""
      ):
        frag(
          st.section(cls := "box")(
            h1(cls := "box__top")(p.title),
            table(cls := "slist slist-pad", id := "version")(
              thead(
                tr(
                  th(colspan := 3)("Current versions"),
                  th(colspan := 2)("Last boot: ", momentFromNow(lila.common.Uptime.startedAt))
                )
              ),
              tbody(
                tr(
                  td("Server"),
                  td(env.appVersionDate),
                  td(a(href := s"https://github.com/lichess-org/lila/commits/$commit")(pre(commit.take(7)))),
                  td(env.appVersionMessage),
                  td(a(href := s"https://github.com/lichess-org/lila/compare/$commit...master")(pre("...")))
                ),
                tr(
                  td("Assets"),
                  td(id := "asset-version-date"),
                  td(a(id := "asset-version-commit")(pre)),
                  td(id := "asset-version-message"),
                  td(a(id := "asset-version-upcoming")(pre("...")))
                )
              )
            )
          ),
          st.section(cls := "box box-pad body")(views.cms.render(p))
        )

  def webmasters(using PageContext) =
    SitePage(
      title = "Webmasters",
      active = "webmasters",
      contentCls = "page force-ltr"
    ).cssTag("page"):
      ui.webmasters(
        li(strong("theme"), ": ", lila.pref.Theme.all.map(_.name).mkString(", ")),
        li(strong("pieceSet"), ": ", lila.pref.PieceSet.all.map(_.name).mkString(", "))
      )

  def SitePage(title: String, active: String, contentCls: String = "")(using lila.ui.Context): Page =
    Page(title).wrap: body =>
      main(cls := "page-menu")(
        ui.menu(active),
        div(cls := s"page-menu__content $contentCls")(body)
      )

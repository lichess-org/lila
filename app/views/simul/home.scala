package views.simul

import lila.app.templating.Environment.{ *, given }

object home:

  def apply(
      pendings: List[lila.simul.Simul],
      opens: List[lila.simul.Simul],
      starteds: List[lila.simul.Simul],
      finisheds: List[lila.simul.Simul]
  )(using ctx: PageContext) =
    views.base.layout(
      moreCss = cssTag("simul.list"),
      moreJs = embedJsUnsafeLoadThen(s"""
site.StrongSocket.defaultParams.flag = 'simul';
site.pubsub.on('socket.in.reload', () =>
  fetch('${routes.Simul.homeReload}').then(r => r.text()).then(html => {
  $$('.simul-list__content').html(html);
  site.contentLoaded();
}))""")(ctx.nonce),
      title = trans.site.simultaneousExhibitions.txt(),
      openGraph = OpenGraph(
        title = trans.site.simultaneousExhibitions.txt(),
        url = s"$netBaseUrl${routes.Simul.home}",
        description = trans.site.aboutSimul.txt()
      ).some,
      withHrefLangs = lila.ui.LangPath(routes.Simul.home).some
    ) {
      main(cls := "page-menu simul-list")(
        st.aside(cls := "page-menu__menu simul-list__help")(
          p(trans.site.aboutSimul()),
          img(src := assetUrl("images/fischer-simul.jpg"), alt := "Simul IRL with Bobby Fischer")(
            em("[1964] ", trans.site.aboutSimulImage()),
            p(trans.site.aboutSimulRealLife()),
            p(trans.site.aboutSimulRules()),
            p(trans.site.aboutSimulSettings())
          )
        ),
        div(cls := "page-menu__content simul-list__content")(
          homeInner(pendings, opens, starteds, finisheds)
        )
      )
    }

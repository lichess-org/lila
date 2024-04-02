package views.html.simul

import controllers.routes

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.*

object home:

  def apply(
      pendings: List[lila.simul.Simul],
      opens: List[lila.simul.Simul],
      starteds: List[lila.simul.Simul],
      finisheds: List[lila.simul.Simul]
  )(using PageContext) =
    views.html.base.layout(
      moreCss = cssTag("simul.list"),
      moreJs = embedJsUnsafeLoadThen(s"""
site.StrongSocket.defaultParams.flag = 'simul';
site.pubsub.on('socket.in.reload', () =>
  fetch('${routes.Simul.homeReload}').then(r => r.text()).then(html => {
  $$('.simul-list__content').html(html);
  site.contentLoaded();
}))"""),
      title = trans.site.simultaneousExhibitions.txt(),
      openGraph = lila.app.ui
        .OpenGraph(
          title = trans.site.simultaneousExhibitions.txt(),
          url = s"$netBaseUrl${routes.Simul.home}",
          description = trans.site.aboutSimul.txt()
        )
        .some,
      withHrefLangs = lila.core.LangPath(routes.Simul.home).some
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

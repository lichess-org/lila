package views.html.simul

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object home {

  def apply(
      pendings: List[lila.simul.Simul],
      opens: List[lila.simul.Simul],
      starteds: List[lila.simul.Simul],
      finisheds: List[lila.simul.Simul]
  )(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = cssTag("simul.list"),
      moreJs = embedJsUnsafeLoadThen(s"""
lichess.StrongSocket.defaultParams.flag = 'simul';
lichess.pubsub.on('socket.in.reload', () =>
  fetch('${routes.Simul.homeReload}').then(r => r.text()).then(html => {
  $$('.simul-list__content').html(html);
  lichess.contentLoaded();
}))"""),
      title = trans.simultaneousExhibitions.txt(),
      openGraph = lila.app.ui
        .OpenGraph(
          title = trans.simultaneousExhibitions.txt(),
          url = s"$netBaseUrl${routes.Simul.home}",
          description = trans.aboutSimul.txt()
        )
        .some
    ) {
      main(cls := "page-menu simul-list")(
        st.aside(cls := "page-menu__menu simul-list__help")(
          p(trans.aboutSimul()),
          img(src := assetUrl("images/fischer-simul.jpg"), alt := "Simul IRL with Bobby Fischer")(
            em("[1964] ", trans.aboutSimulImage()),
            p(trans.aboutSimulRealLife()),
            p(trans.aboutSimulRules()),
            p(trans.aboutSimulSettings())
          )
        ),
        div(cls := "page-menu__content simul-list__content")(
          homeInner(pendings, opens, starteds, finisheds)
        )
      )
    }
}

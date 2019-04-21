package views.html.simul

import play.api.libs.json.Json

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object home {

  def apply(
    opens: List[lidraughts.simul.Simul],
    starteds: List[lidraughts.simul.Simul],
    finisheds: List[lidraughts.simul.Simul]
  )(implicit ctx: Context) = views.html.base.layout(
    moreCss = cssTag("simul.list"),
    moreJs = embedJs(s"""$$(function() {
  lidraughts.StrongSocket.defaults.params.flag = 'simul';
  lidraughts.pubsub.on('socket.in.reload', () => {
    $$('.simul-list__content').load('${routes.Simul.homeReload()}', lidraughts.pubsub.emit('content_loaded'));
  });
});"""),
    title = trans.simultaneousExhibitions.txt(),
    openGraph = lidraughts.app.ui.OpenGraph(
      title = trans.simultaneousExhibitions.txt(),
      url = s"$netBaseUrl${routes.Simul.home}",
      description = trans.aboutSimul.txt()
    ).some
  ) {
      main(cls := "page-menu simul-list")(
        st.aside(cls := "page-menu__menu simul-list__help")(
          p(trans.aboutSimul()),
          img(src := staticUrl("images/sijbrands-simul.jpg"), alt := "Simul IRL with Ton Sijbrands")(
            em("[1967] ", trans.aboutSimulImage.frag()),
            p(trans.aboutSimulRealLife.frag()),
            p(trans.aboutSimulRules.frag()),
            p(trans.aboutSimulSettings.frag())
          )
        ),
        div(cls := "page-menu__content simul-list__content")(
          homeInner(opens, starteds, finisheds)
        )
      )
    }
}

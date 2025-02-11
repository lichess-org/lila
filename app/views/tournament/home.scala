package views.html.tournament

import controllers.routes
import play.api.mvc.Call

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.tournament.Tournament
import lila.tournament.TournamentPager.Order

object home {

  def apply(
      pag: Paginator[Tournament],
      order: Order,
      winners: lila.tournament.AllWinners,
  )(implicit ctx: Context) = {
    val url = (o: String) => routes.Tournament.home(o)
    views.html.base.layout(
      title = trans.tournaments.txt(),
      moreCss = cssTag("tournament.home"),
      wrapClass = "full-screen-force",
      moreJs = frag(infiniteScrollTag),
      openGraph = lila.app.ui
        .OpenGraph(
          url = s"$netBaseUrl${routes.Tournament.homeDefault(1).url}",
          title = trans.tournamentHomeTitle.txt(),
          description = trans.tournamentHomeDescription.txt(),
        )
        .some,
      withHrefLangs = lila.i18n.LangList.All.some,
    ) {
      main(cls := "page-menu tour-home")(
        menu("home"),
        st.section(cls := "page-menu__content tour-home__list box")(
          div(cls := "tour-winners-wrap")(
            div(cls := "tour-winners")(
              winners.top.map { w =>
                val name = winnerTournamentName(w)
                div(
                  userIdLink(w.userId.some),
                  a(cls := "tourname", title := name, href := routes.Tournament.show(w.tourId))(
                    name,
                  ),
                )
              },
            ),
          ),
          div(cls := "box__top")(
            h1(trans.tournaments()),
            boxActions("home", order, url),
          ),
          views.html.tournament.list.paginate(pag, routes.Tournament.home(order.key).url),
        ),
      )
    }
  }

  def finished(
      pag: Paginator[Tournament],
      order: Order,
  )(implicit ctx: Context) = {
    val url = (o: String) => routes.Tournament.finished(o)
    views.html.base.layout(
      title = trans.tournaments.txt(),
      moreCss = cssTag("tournament.home"),
      wrapClass = "full-screen-force",
      moreJs = frag(infiniteScrollTag),
    ) {
      main(cls := "page-menu tour-home")(
        menu("finished"),
        st.section(cls := "page-menu__content tour-home__list box")(
          div(cls := "box__top")(
            h1(trans.finished()),
            boxActions("finished", order, url),
          ),
          views.html.tournament.list.paginate(pag, routes.Tournament.finished(order.key).url),
        ),
      )
    }
  }

  private def boxActions(active: String, order: Order, url: String => Call)(implicit ctx: Context) =
    div(cls := "box__top__actions")(
      bits.orderSelect(order, active, url),
      ctx.isAuth option a(
        href     := routes.Tournament.form,
        cls      := "button button-green text",
        dataIcon := "O",
      )(trans.createANewTournament()),
    )

  def menu(active: String)(implicit ctx: Context) =
    st.aside(cls := "page-menu__menu subnav")(
      a(cls := active.active("home"), href := routes.Tournament.homeDefault(1))(
        trans.tournaments(),
      ),
      a(cls := active.active("finished"), href := routes.Tournament.finished(Order.Started.key, 1))(
        trans.finished(),
      ),
      ctx.me map { me =>
        a(cls := active.active("user"), href := routes.UserTournament.path(me.username, "created"))(
          trans.myTournaments(),
        )
      },
      a(cls := active.active("calendar"), href := routes.Tournament.calendar)(
        trans.tournamentCalendar(),
      ),
      a(cls := active.active("faq"), href := routes.Tournament.help("arena".some))(
        trans.faq.faqAbbreviation(),
      ),
    )
}

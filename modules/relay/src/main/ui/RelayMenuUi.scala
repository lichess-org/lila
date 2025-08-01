package lila.relay
package ui

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.core.LightUser

final class RelayMenuUi(helpers: Helpers):
  import helpers.{ *, given }
  import trans.broadcast as trc

  def apply(menu: String, by: Option[LightUser] = none)(using ctx: Context): Tag =
    lila.ui.bits.pageMenuSubnav(
      a(href := routes.RelayTour.index(), cls := menu.activeO("index"))(trc.broadcasts()),
      ctx.me.map: me =>
        a(
          href := routes.RelayTour.by(me.username, 1),
          cls := (menu == "new" || by.exists(_.is(me))).option("active")
        ):
          trc.myBroadcasts()
      ,
      by.filterNot(ctx.is)
        .map: user =>
          a(href := routes.RelayTour.by(user.name, 1), cls := "active")(
            user.name,
            " ",
            trc.broadcasts()
          ),
      a(href := routes.RelayTour.subscribed(), cls := menu.activeO("subscribed"))(
        trc.subscribedBroadcasts()
      ),
      Granter
        .opt(_.StudyAdmin)
        .option(
          a(href := routes.RelayTour.allPrivate(), cls := menu.activeO("allPrivate"))(
            "Private Broadcasts"
          )
        ),
      a(href := routes.RelayTour.calendar, cls := menu.activeO("calendar"))(trc.broadcastCalendar()),
      a(href := routes.RelayTour.help, cls := menu.activeO("help"))(trc.aboutBroadcasts()),
      a(href := routes.RelayTour.app, cls := menu.activeO("app"))("Broadcaster App"),
      div(cls := "sep"),
      a(cls := menu.active("players"), href := routes.Fide.index())(trc.fidePlayers()),
      a(cls := menu.active("federations"), href := routes.Fide.federations(1))(
        trc.fideFederations()
      )
    )

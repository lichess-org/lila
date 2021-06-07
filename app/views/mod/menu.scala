package views.html.mod

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object menu {

  def apply(active: String)(implicit ctx: Context) =
    st.nav(cls := "page-menu__menu subnav")(
      isGranted(_.SeeReport) option
        a(cls := active.active("report"), href := routes.Report.list)("Reports"),
      isGranted(_.PublicChatView) option
        a(cls := active.active("public-chat"), href := routes.Mod.publicChat)("Public Chats"),
      isGranted(_.GamifyView) option
        a(cls := active.active("progress"), href := routes.Mod.progress)("Mod progress"),
      isGranted(_.GamifyView) option
        a(cls := active.active("gamify"), href := routes.Mod.gamify)("Hall of fame"),
      isGranted(_.UserSearch) option
        a(cls := active.active("search"), href := routes.Mod.search)("Search users"),
      isGranted(_.SetEmail) option
        a(cls := active.active("email"), href := routes.Mod.emailConfirm)("Email confirm"),
      isGranted(_.PracticeConfig) option
        a(cls := active.active("practice"), href := routes.Practice.config)("Practice"),
      isGranted(_.ManageTournament) option
        a(cls := active.active("tour"), href := routes.TournamentCrud.index(1))("Tournaments"),
      isGranted(_.ManageEvent) option
        a(cls := active.active("event"), href := routes.Event.manager)("Events"),
      isGranted(_.SeeReport) option
        a(cls := active.active("irwin"), href := routes.Irwin.dashboard)("Irwin dashboard"),
      isGranted(_.Shadowban) option
        a(cls := active.active("panic"), href := routes.Mod.chatPanic)(
          "Chat Panic: ",
          strong(if (isChatPanicEnabled) "ON" else "OFF")
        ),
      isGranted(_.Admin) option
        a(cls := active.active("mods"), href := routes.Mod.table)("Mods"),
      isGranted(_.Presets) option
        a(cls := active.active("presets"), href := routes.Mod.presets("PM"))("Msg presets"),
      isGranted(_.Settings) option
        a(cls := active.active("setting"), href := routes.Dev.settings)("Settings"),
      isGranted(_.Cli) option
        a(cls := active.active("cli"), href := routes.Dev.cli)("CLI")
    )
}

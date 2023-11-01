package views.html.mod

import controllers.report.routes.{ Report as reportRoutes }
import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

object menu:

  def apply(active: String)(using PageContext) =
    views.html.site.bits.pageMenuSubnav(
      isGranted(_.SeeReport) option
        a(cls := active.active("report"), href := reportRoutes.list)("Reports"),
      isGranted(_.PublicChatView) option
        a(cls := active.active("public-chat"), href := routes.Mod.publicChat)("Public Chats"),
      isGranted(_.GamifyView) option
        a(cls := active.active("activity"), href := routes.Mod.activity)("Mod activity"),
      isGranted(_.GamifyView) option
        a(cls := active.active("queues"), href := routes.Mod.queues("month"))("Queues stats"),
      isGranted(_.GamifyView) option
        a(cls := active.active("gamify"), href := routes.Mod.gamify)("Hall of fame"),
      isGranted(_.GamifyView) option
        a(cls := active.active("log"), href := routes.Mod.log)("My logs"),
      isGranted(_.UserSearch) option
        a(cls := active.active("search"), href := routes.Mod.search)("Search users"),
      isGranted(_.Admin) option
        a(cls := active.active("notes"), href := routes.Mod.notes())("Mod notes"),
      isGranted(_.SetEmail) option
        a(cls := active.active("email"), href := routes.Mod.emailConfirm)("Email confirm"),
      isGranted(_.PracticeConfig) option
        a(cls := active.active("practice"), href := routes.Practice.config)("Practice"),
      isGranted(_.ManageTournament) option
        a(cls := active.active("tour"), href := routes.TournamentCrud.index(1))("Tournaments"),
      isGranted(_.ManageEvent) option
        a(cls := active.active("event"), href := routes.Event.manager)("Events"),
      isGranted(_.MarkEngine) option
        a(cls := active.active("irwin"), href := routes.Irwin.dashboard)("Irwin dashboard"),
      isGranted(_.MarkEngine) option
        a(cls := active.active("kaladin"), href := routes.Irwin.kaladin)("Kaladin dashboard"),
      isGranted(_.Shadowban) option
        a(cls := active.active("panic"), href := routes.Mod.chatPanic)(
          "Chat Panic: ",
          strong(if isChatPanicEnabled then "ON" else "OFF")
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

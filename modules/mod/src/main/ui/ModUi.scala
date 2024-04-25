package lila.mod
package ui

import play.api.data.Form

import lila.ui.*
import ScalatagsTemplate.{ *, given }
import lila.report.{ Report, Reason }
import lila.core.perm.Permission

final class ModUi(helpers: Helpers)(
    isChatPanic: () => Boolean
):
  import helpers.{ *, given }

  def gdprEraseButton(u: User)(using Context) =
    val allowed = u.marks.clean || Granter.opt(_.Admin)
    submitButton(
      cls := (!allowed).option("disabled"),
      title := {
        if allowed
        then "Definitely erase everything about this user"
        else "This user has some history, only admins can erase"
      },
      (!allowed).option(disabled)
    )("GDPR erasure")

  def myLogs(logs: List[lila.mod.Modlog])(using Context) =
    main(cls := "page-menu")(
      menu("log"),
      div(id := "modlog_table", cls := "page-menu__content box")(
        h1(cls := "box__top")("My logs"),
        table(cls := "slist slist-pad")(
          thead(
            tr(
              th("Date"),
              th("User"),
              th("Action"),
              th("Details")
            )
          ),
          tbody(
            logs.map { log =>
              tr(
                td(momentFromNow(log.date)),
                td(log.user.map { u =>
                  userIdLink(u.some, params = "?mod")
                }),
                td(log.showAction.capitalize),
                td(log.details)
              )
            }
          )
        )
      )
    )

  def permissions(u: User, permissions: List[(String, List[Permission])])(using ctx: Context, me: Me) =

    def findGranterPackage(perms: Set[Permission], perm: Permission): Option[Permission] =
      (!perms(perm)).so(perms.find(_.grants(perm)))

    main(cls := "mod-permissions page-small box box-pad")(
      boxTop(h1(userLink(u), " permissions")),
      standardFlash,
      postForm(cls := "form3", action := routes.Mod.permissions(u.username))(
        p(cls := "granted")("In green, permissions enabled manually or by a package."),
        div(cls := "permission-list")(
          permissions
            .filter { (_, ps) => ps.exists(canGrant(_)) }
            .map: (categ, perms) =>
              st.section(
                h2(categ),
                perms
                  .filter(canGrant)
                  .map: perm =>
                    val id = s"permission-${perm.dbKey}"
                    div(
                      cls := Granter.of(perm)(u).option("granted"),
                      title := Granter
                        .of(perm)(u)
                        .so:
                          findGranterPackage(Permission(u), perm).map: p =>
                            s"Granted by package: $p"
                    )(
                      span(
                        form3.cmnToggle(
                          id,
                          "permissions[]",
                          checked = u.roles.contains(perm.dbKey),
                          value = perm.dbKey
                        )
                      ),
                      label(`for` := id)(perm.name)
                    )
              )
        ),
        form3.actions(
          a(href := routes.User.show(u.username))(trans.site.cancel()),
          submitButton(cls := "button")(trans.site.save())
        )
      )
    )

  def chatPanic(state: Option[Instant])(using Context) =
    main(cls := "page-menu")(
      menu("panic"),
      div(id := "chat-panic", cls := "page-menu__content box box-pad")(
        h1(cls := "box__top")("Chat Panic"),
        p(
          "When Chat Panic is enabled, restrictions apply to public chats (tournament, simul) and PM",
          br,
          "Only players 24h old, and with 10 games played, can write messages."
        ),
        p(
          "Current state: ",
          state
            .map: s =>
              frag(
                goodTag(cls := "text", dataIcon := Icon.Checkmark)(strong("ENABLED")),
                ". Expires ",
                momentFromNow(s)
              )
            .getOrElse(badTag(cls := "text", dataIcon := Icon.X)(strong("DISABLED")))
        ),
        div(cls := "forms")(
          if state.isDefined then
            frag(
              postForm(action := s"${routes.Mod.chatPanicPost}?v=0")(
                submitButton(cls := "button button-fat button-red text", dataIcon := Icon.X)("Disable")
              ),
              postForm(action := s"${routes.Mod.chatPanicPost}?v=1")(
                submitButton(cls := "button button-fat button-green text", dataIcon := Icon.Checkmark)(
                  "Renew for two hours"
                )
              )
            )
          else
            postForm(action := s"${routes.Mod.chatPanicPost}?v=1")(
              submitButton(cls := "button button-fat text", dataIcon := Icon.Checkmark)("Enable")
            )
        )
      )
    )

  def presets(group: String, form: Form[?])(using Context) =
    main(cls := "page-menu")(
      menu("presets"),
      div(cls := "page-menu__content box box-pad mod-presets")(
        boxTop(
          h1(
            s"$group presets",
            small(
              " / ",
              ModPresets.groups.filter(group !=).map { group =>
                a(href := routes.Mod.presets(group))(s"$group presets")
              }
            )
          )
        ),
        standardFlash,
        postForm(action := routes.Mod.presetsUpdate(group))(
          form3.group(
            form("v"),
            raw(""),
            help = frag(
              "First line is the permissions needed to use the preset (If a list, separated by commas is given, any user having at least one of these permissions will be able to send it), second is the preset name, next lines are the content. Separate presets with a line of 3 or more dashes: ---."
            ).some
          )(form3.textarea(_)(rows := 20)),
          form3.action(
            submitButton(cls := "button text", dataIcon := Icon.Checkmark)("Save")
          )
        )
      )
    )

  def menu(active: String)(using Context): Frag =
    lila.ui.bits.pageMenuSubnav(
      Granter
        .opt(_.SeeReport)
        .option(a(cls := active.active("report"), href := routes.Report.list)("Reports")),
      Granter
        .opt(_.PublicChatView)
        .option(a(cls := active.active("public-chat"), href := routes.Mod.publicChat)("Public Chats")),
      Granter
        .opt(_.GamifyView)
        .option(a(cls := active.active("activity"), href := routes.Mod.activity)("Mod activity")),
      Granter
        .opt(_.GamifyView)
        .option(a(cls := active.active("queues"), href := routes.Mod.queues("month"))("Queues stats")),
      Granter
        .opt(_.GamifyView)
        .option(a(cls := active.active("gamify"), href := routes.Mod.gamify)("Hall of fame")),
      Granter.opt(_.GamifyView).option(a(cls := active.active("log"), href := routes.Mod.log)("My logs")),
      Granter
        .opt(_.UserSearch)
        .option(a(cls := active.active("search"), href := routes.Mod.search)("Search users")),
      Granter.opt(_.Admin).option(a(cls := active.active("notes"), href := routes.Mod.notes())("Mod notes")),
      Granter
        .opt(_.SetEmail)
        .option(a(cls := active.active("email"), href := routes.Mod.emailConfirm)("Email confirm")),
      Granter.opt(_.Pages).option(a(cls := active.active("cms"), href := routes.Cms.index)("Pages")),
      Granter
        .opt(_.PracticeConfig)
        .option(a(cls := active.active("practice"), href := routes.Practice.config)("Practice")),
      Granter
        .opt(_.ManageTournament)
        .option(a(cls := active.active("tour"), href := routes.TournamentCrud.index(1))("Tournaments")),
      Granter
        .opt(_.ManageEvent)
        .option(a(cls := active.active("event"), href := routes.Event.manager)("Events")),
      Granter
        .opt(_.MarkEngine)
        .option(a(cls := active.active("irwin"), href := routes.Irwin.dashboard)("Irwin dashboard")),
      Granter
        .opt(_.MarkEngine)
        .option(a(cls := active.active("kaladin"), href := routes.Irwin.kaladin)("Kaladin dashboard")),
      Granter
        .opt(_.Shadowban)
        .option(
          a(cls := active.active("panic"), href := routes.Mod.chatPanic)(
            "Chat Panic: ",
            strong(if isChatPanic() then "ON" else "OFF")
          )
        ),
      Granter.opt(_.Admin).option(a(cls := active.active("mods"), href := routes.Mod.table)("Mods")),
      Granter
        .opt(_.Presets)
        .option(a(cls := active.active("presets"), href := routes.Mod.presets("PM"))("Msg presets")),
      Granter
        .opt(_.Settings)
        .option(a(cls := active.active("setting"), href := routes.Dev.settings)("Settings")),
      Granter.opt(_.Cli).option(a(cls := active.active("cli"), href := routes.Dev.cli)("CLI"))
    )

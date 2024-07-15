package lila.mod
package ui

import play.api.data.Form
import play.api.libs.json.Json

import lila.core.perf.UserWithPerfs
import lila.core.perm.Permission
import lila.mod.ModActivity.{ Period, Who }
import lila.ui.*

import ScalatagsTemplate.{ *, given }

case class PendingCounts(streamers: Int, appeals: Int, titles: Int)

final class ModUi(helpers: Helpers)(isChatPanic: () => Boolean):
  import helpers.{ *, given }

  def impersonate(user: User)(using Translate) =
    div(id := "impersonate")(
      div(cls := "meat")("You are impersonating ", userLink(user, withOnline = false)),
      div(cls := "actions")(
        postForm(action := routes.Mod.impersonate("-"))(
          submitButton(cls := "button button-empty")("Quit")
        )
      )
    )

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
    Page("My logs").css("mod.misc"):
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
              logs.map: log =>
                tr(
                  td(momentFromNow(log.date)),
                  td(log.user.map { u =>
                    userIdLink(u.some, params = "?mod")
                  }),
                  td(log.showAction.capitalize),
                  td(log.details)
                )
            )
          )
        )
      )

  def permissions(u: User, permissions: List[(String, List[Permission])])(using ctx: Context, me: Me) =
    def findGranterPackage(perms: Set[Permission], perm: Permission): Option[Permission] =
      (!perms(perm)).so(perms.find(_.grants(perm)))
    Page(s"${u.username} permissions").css("mod.permission", "bits.form3"):
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
    Page("Chat Panic").css("mod.misc"):
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
    Page(s"$group presets").css("mod.misc", "bits.form3"):
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

  val emailConfirmJs = """$('.mod-confirm form input').on('paste', function() {
setTimeout(function() { $(this).parent().submit(); }.bind(this), 50);
}).each(function() {
this.setSelectionRange(this.value.length, this.value.length);
});"""

  def emailConfirm(query: String, user: Option[UserWithPerfs], email: Option[EmailAddress])(using
      ctx: Context
  ) =
    Page("Email confirmation")
      .css("mod.misc")
      .js(embedJsUnsafeLoadThen(emailConfirmJs)):
        main(cls := "page-menu")(
          menu("email"),
          div(cls := "mod-confirm page-menu__content box box-pad")(
            h1(cls := "box__top")("Confirm a user email"),
            p(
              "If you provide an email, it will confirm the corresponding account, if any.",
              br,
              "If you provide an email and a username, it will set the email to that user, ",
              "but only if the user has not yet confirmed their email."
            ),
            st.form(cls := "search", action := routes.Mod.emailConfirm, method := "GET")(
              input(name := "q", placeholder := "<email> <username (optional)>", value := query, autofocus)
            ),
            user.map: u =>
              table(cls := "slist")(
                thead:
                  tr(
                    th("User"),
                    th("Email"),
                    th("Games"),
                    th("Marks"),
                    th("Created"),
                    th("Active"),
                    th("Confirmed")
                  )
                ,
                tbody:
                  tr(
                    td(userLink(u.user, withPerfRating = u.perfs.some, params = "?mod")),
                    td(email.fold("-")(_.value)),
                    td(u.count.game.localize),
                    td(
                      u.marks.engine.option("ENGINE"),
                      u.marks.boost.option("BOOSTER"),
                      u.marks.troll.option("SHADOWBAN"),
                      u.enabled.no.option("CLOSED")
                    ),
                    td(momentFromNow(u.createdAt)),
                    td(u.seenAt.map(momentFromNow(_))),
                    td(style := "font-size:2em")(
                      if !u.everLoggedIn then iconTag(Icon.Checkmark)(cls := "is-green")
                      else iconTag(Icon.X)(cls                            := "is-red")
                    )
                  )
              )
          )
        )

  def queueStats(p: ModQueueStats.Result)(using Context) =
    Page("Queues stats")
      .css("mod.activity")
      .js(PageModule("mod.activity", Json.obj("op" -> "queues", "data" -> p.json))):
        main(cls := "page-menu")(
          menu("queues"),
          div(cls := "page-menu__content index box mod-queues")(
            boxTop(
              h1(
                " Queues this ",
                lila.ui.bits.mselect(
                  s"mod-activity__period-select box__top__actions",
                  span(p.period.key),
                  Period.values.toList.map: per =>
                    a(
                      cls  := (p.period == per).option("current"),
                      href := routes.Mod.queues(per.key)
                    )(per.toString)
                )
              )
            ),
            div(cls := "chart-grid")
          )
        )

  def activity(p: ModActivity.Result)(using Context) =
    val whoSelector = lila.ui.bits.mselect(
      s"mod-activity__who-select box__top__actions",
      span(if p.who == Who.Team then "Team" else "My"),
      List(
        a(
          cls  := (p.who == Who.Team).option("current"),
          href := routes.Mod.activityOf("team", p.period.key)
        )("Team"),
        a(
          cls  := (p.who != Who.Team).option("current"),
          href := routes.Mod.activityOf("me", p.period.key)
        )("My")
      )
    )
    val periodSelector = lila.ui.bits.mselect(
      s"mod-activity__period-select box__top__actions",
      span(p.period.key),
      Period.values.toList.map { per =>
        a(
          cls  := (p.period == per).option("current"),
          href := routes.Mod.activityOf(p.who.key, per.key)
        )(per.toString)
      }
    )
    Page("Moderation activity")
      .css("mod.activity")
      .js(PageModule("mod.activity", Json.obj("op" -> "activity", "data" -> ModActivity.json(p)))):
        main(cls := "page-menu")(
          menu("activity"),
          div(cls := "page-menu__content index box mod-activity")(
            boxTop(h1(whoSelector, " activity this ", periodSelector)),
            div(cls := "chart chart-reports"),
            div(cls := "chart chart-actions")
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

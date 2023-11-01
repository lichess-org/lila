package views.html.team

import controllers.routes
import play.api.data.Form

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.team.{ Team, TeamMember }

object form:

  import trans.team.*

  def create(form: Form[?], captcha: lila.common.Captcha)(using PageContext) =
    views.html.base.layout(
      title = newTeam.txt(),
      moreCss = cssTag("team"),
      moreJs = captchaTag
    ) {
      main(cls := "page-menu page-small")(
        bits.menu("form".some),
        div(cls := "page-menu__content box box-pad")(
          h1(cls := "box__top")(newTeam()),
          postForm(cls := "form3", action := routes.Team.create)(
            form3.globalError(form),
            form3.group(form("name"), trans.name())(form3.input(_)),
            entryFields(form, none),
            textFields(form),
            views.html.base.captcha(form, captcha),
            form3.actions(
              a(href := routes.Team.home(1))(trans.cancel()),
              form3.submit(newTeam())
            )
          )
        )
      )
    }

  def edit(t: Team, form: Form[?], member: Option[TeamMember])(using ctx: PageContext) =
    bits.layout(title = s"Edit Team ${t.name}", moreJs = jsModule("team")) {
      main(cls := "page-menu page-small team-edit")(
        bits.menu(none),
        div(cls := "page-menu__content box box-pad")(
          boxTop(h1("Edit team ", a(href := routes.Team.show(t.id))(t.name))),
          standardFlash,
          t.enabled option postForm(cls := "form3", action := routes.Team.update(t.id))(
            entryFields(form, t.some),
            textFields(form),
            accessFields(form),
            form3.actions(
              a(href := routes.Team.show(t.id), style := "margin-left:20px")(trans.cancel()),
              form3.submit(trans.apply())
            )
          ),
          hr,
          (t.enabled && (member.exists(_.hasPerm(_.Admin)) || isGranted(_.ManageTeam))) option
            postForm(cls := "inline", action := routes.Team.disable(t.id))(
              explainInput,
              submitButton(
                dataIcon := licon.CautionCircle,
                cls      := "submit button text explain button-empty button-red",
                st.title := trans.team.closeTeamDescription.txt() // can actually be reverted
              )(closeTeam())
            ),
          isGranted(_.ManageTeam) option
            postForm(cls := "inline", action := routes.Team.close(t.id))(
              explainInput,
              submitButton(
                dataIcon := licon.Trash,
                cls      := "text button button-empty button-red explain",
                st.title := "Deletes the team and its memberships. Cannot be reverted!"
              )(trans.delete())
            ),
          (t.disabled && isGranted(_.ManageTeam)) option
            postForm(cls := "inline", action := routes.Team.disable(t.id))(
              explainInput,
              submitButton(
                cls      := "button button-empty explain",
                st.title := "Re-enables the team and restores memberships"
              )("Re-enable")
            )
        )
      )
    }

  private val explainInput = input(st.name := "explain", tpe := "hidden")

  private def textFields(form: Form[?])(using Context) = frag(
    form3.group(
      form("intro"),
      "Introduction",
      help = frag("Brief description visible in team listings. Up to 200 chars.").some
    )(
      form3.textarea(_)(rows := 2)
    )(cls := form("intro").value.isEmpty.option("accent")),
    form3.group(
      form("description"),
      trans.description(),
      help = frag("Full description visible on the team page.", br, markdownAvailable).some
    )(
      form3.textarea(_)(rows := 10)
    ),
    form3.group(
      form("descPrivate"),
      trans.descPrivate(),
      help = frag(
        trans.descPrivateHelp(),
        br,
        markdownAvailable
      ).some
    )(
      form3.textarea(_)(rows := 10)
    )
  )

  private def accessFields(form: Form[?])(using Context) =
    frag(
      form3.checkbox(
        form("hideMembers"),
        "Hide team member list from non-members.",
        half = true
      ),
      form3.split(
        form3.group(form("chat"), frag("Team chat"), help = frag("Who can use the team chat?").some) { f =>
          form3.select(
            f,
            Seq(
              Team.Access.NONE    -> "No chat",
              Team.Access.LEADERS -> "Team leaders",
              Team.Access.MEMBERS -> "Team members"
            )
          )
        },
        form3.group(
          form("forum"),
          frag("Team forum"),
          help = frag(
            "Who can see the team forum on the team page?",
            br,
            "Only team members can post in the team forum."
          ).some
        ) { f =>
          form3.select(
            f,
            Seq(
              Team.Access.EVERYONE -> "Show to everyone",
              Team.Access.MEMBERS  -> "Show to members",
              Team.Access.LEADERS  -> "Show to team leaders",
              Team.Access.NONE     -> "Hide the forum"
            )
          )
        }
      )
    )

  private def entryFields(form: Form[?], team: Option[Team])(using ctx: Context) =
    form3.split(
      form3.checkbox(
        form("request"),
        trans.team.manuallyReviewAdmissionRequests(),
        help = trans.team.manuallyReviewAdmissionRequestsHelp().some,
        half = true
      ),
      form3.group(
        form("password"),
        trans.team.entryCode(),
        help = trans.team.entryCodeDescriptionForLeader().some,
        half = true
      )(form3.input(_))
    )

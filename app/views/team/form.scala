package views.html.team

import controllers.routes
import play.api.data.Form
import play.api.i18n.Lang

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.team.Team

object form {

  import trans.team._

  def create(form: Form[_], captcha: lila.common.Captcha)(implicit ctx: Context) =
    views.html.base.layout(
      title = newTeam.txt(),
      moreCss = cssTag("team"),
      moreJs = captchaTag
    ) {
      main(cls := "page-menu page-small")(
        bits.menu("form".some),
        div(cls := "page-menu__content box box-pad")(
          h1(newTeam()),
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

  def edit(t: Team, form: Form[_])(implicit ctx: Context) = {
    val title = "Edit Team " + t.name
    bits.layout(title = title) {
      main(cls := "page-menu page-small team-edit")(
        bits.menu(none),
        div(cls := "page-menu__content box box-pad")(
          h1(title),
          t.enabled option postForm(cls := "form3", action := routes.Team.update(t.id))(
            div(cls := "form-group")(
              a(cls := "button button-empty", href := routes.Team.leaders(t.id))(teamLeaders()),
              a(cls := "button button-empty", href := routes.Team.kick(t.id))(kickSomeone())
            ),
            entryFields(form, t.some),
            textFields(form),
            accessFields(form),
            form3.actions(
              a(href := routes.Team.show(t.id), style := "margin-left:20px")(trans.cancel()),
              form3.submit(trans.apply())
            )
          ),
          ctx.userId.exists(t.leaders) || isGranted(_.ManageTeam) option frag(
            hr,
            t.enabled option postForm(cls := "inline", action := routes.Team.disable(t.id))(
              submitButton(
                dataIcon := "",
                cls := "submit button text confirm button-empty button-red",
                st.title := trans.team.closeTeamDescription.txt() // can actually be reverted
              )(closeTeam())
            ),
            isGranted(_.ManageTeam) option
              postForm(cls := "inline", action := routes.Team.close(t.id))(
                submitButton(
                  dataIcon := "",
                  cls := "text button button-empty button-red confirm",
                  st.title := "Deletes the team and its memberships. Cannot be reverted!"
                )(trans.delete())
              ),
            (t.disabled && isGranted(_.ManageTeam)) option
              postForm(cls := "inline", action := routes.Team.disable(t.id))(
                submitButton(
                  cls := "button button-empty confirm",
                  st.title := "Re-enables the team and restores memberships"
                )("Re-enable")
              )
          )
        )
      )
    }
  }

  private def textFields(form: Form[_])(implicit ctx: Context) = frag(
    form3.group(form("description"), trans.description(), help = markdownAvailable.some)(
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

  private def accessFields(form: Form[_])(implicit ctx: Context) =
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
            "Note that the forum remains accessible through direct URL access or forum search.",
            br,
            "Only team members can post in the team forum."
          ).some
        ) { f =>
          form3.select(
            f,
            Seq(
              Team.Access.NONE    -> "Hide the forum",
              Team.Access.LEADERS -> "Show to team leaders",
              Team.Access.MEMBERS -> "Show to members"
            )
          )
        }
      )
    )

  private def entryFields(form: Form[_], team: Option[Team])(implicit ctx: Context) =
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
      ) { field =>
        if (team.fold(true)(t => ctx.userId.exists(t.leaders.contains)))
          form3.input(field)
        else form3.input(field)(tpe := "password", disabled)
      }
    )
}

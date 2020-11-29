package views.html.team

import play.api.data.Form

import lila.team.Team
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

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
          postForm(cls := "form3", action := routes.Team.create())(
            form3.globalError(form),
            form3.group(form("name"), trans.name())(form3.input(_)),
            form3.group(form("open"), joiningPolicy()) { _ =>
              form3.select(
                form("open"),
                Seq(0 -> aConfirmationIsRequiredToJoin.txt(), 1 -> anyoneCanJoin.txt())
              )
            },
            form3.group(form("location"), trans.location())(form3.input(_)),
            form3.group(form("description"), trans.description())(form3.textarea(_)(rows := 10)),
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
            form3.group(form("open"), joiningPolicy()) { f =>
              form3.select(
                f,
                Seq(0 -> aConfirmationIsRequiredToJoin.txt(), 1 -> anyoneCanJoin.txt())
              )
            },
            form3.group(form("location"), trans.location())(form3.input(_)),
            form3.group(form("description"), trans.description())(form3.textarea(_)(rows := 10)),
            form3.group(form("chat"), frag("Team chat")) { f =>
              form3.select(
                f,
                Seq(
                  Team.ChatFor.NONE    -> "No chat",
                  Team.ChatFor.LEADERS -> "Team leaders",
                  Team.ChatFor.MEMBERS -> "Team members"
                )
              )
            },
            form3.actions(
              a(href := routes.Team.show(t.id), style := "margin-left:20px")(trans.cancel()),
              form3.submit(trans.apply())
            )
          ),
          ctx.userId.exists(t.leaders) || isGranted(_.ManageTeam) option frag(
            hr,
            t.enabled option postForm(cls := "inline", action := routes.Team.disable(t.id))(
              submitButton(
                dataIcon := "j",
                cls := "submit button text confirm button-red",
                st.title := trans.team.closeTeamDescription.txt() // can actually be reverted
              )(closeTeam())
            ),
            isGranted(_.ManageTeam) option
              postForm(cls := "inline", action := routes.Team.close(t.id))(
                submitButton(
                  dataIcon := "q",
                  cls := "text button button-empty button-red confirm",
                  st.title := "Deletes the team and its memberships. Cannot be reverted!"
                )(trans.delete())
              )
          )
        )
      )
    }
  }
}

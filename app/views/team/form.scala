package views.html.team

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object form {

  def create(form: Form[_], captcha: lila.common.Captcha)(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.newTeam.txt(),
      moreCss = cssTag("team"),
      moreJs = frag(infiniteScrollTag, captchaTag)
    ) {
        main(cls := "page-menu page-small")(
          bits.menu("form".some),
          div(cls := "page-menu__content box box-pad")(
            h1(trans.newTeam()),
            st.form(cls := "form3", action := routes.Team.create(), method := "POST")(
              form3.globalError(form),
              form3.group(form("name"), trans.name())(form3.input(_)),
              form3.group(form("open"), trans.joiningPolicy()) { f =>
                form3.select(form("open"), Seq(0 -> trans.aConfirmationIsRequiredToJoin.txt(), 1 -> trans.anyoneCanJoin.txt()))
              },
              form3.group(form("location"), trans.location())(form3.input(_)),
              form3.group(form("description"), trans.description())(form3.textarea(_)(rows := 10)),
              views.html.base.captcha(form, captcha),
              form3.actions(
                a(href := routes.Team.home(1))(trans.cancel()),
                form3.submit(trans.newTeam())
              )
            )
          )
        )
      }

  def edit(t: lila.team.Team, form: Form[_])(implicit ctx: Context) = {
    val title = "Edit Team " + t.name
    bits.layout(title = title) {
      main(cls := "page-menu page-small")(
        bits.menu(none),
        div(cls := "page-menu__content box box-pad")(
          h1(title),
          st.form(cls := "form3", action := routes.Team.update(t.id), method := "POST")(
            div(cls := "form-group")(
              a(cls := "button button-empty", href := routes.Team.kick(t.id))("Kick someone out of the team"),
              a(cls := "button button-empty", href := routes.Team.changeOwner(t.id))("Appoint another team owner")
            ),
            form3.group(form("open"), trans.joiningPolicy()) { f =>
              form3.select(f, Seq(0 -> trans.aConfirmationIsRequiredToJoin.txt(), 1 -> trans.anyoneCanJoin.txt()))
            },
            form3.group(form("location"), trans.location())(form3.input(_)),
            form3.group(form("description"), trans.description())(form3.textarea(_)(rows := 10)),
            form3.actions(
              a(href := routes.Team.show(t.id), style := "margin-left:20px")(trans.cancel()),
              form3.submit(trans.apply())
            )
          ),
          isGranted(_.ManageTeam) option frag(
            hr,
            st.form(cls := "inline", method := "post", action := routes.Team.close(t.id))(
              button(dataIcon := "q", cls := "text button button-empty button-red confirm", tpe := "submit",
                st.title := "Deletes the team and its memberships. Cannot be reverted!")("Delete")
            )
          )
        )
      )
    }
  }
}

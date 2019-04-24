package views.html.message

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.common.String.html.safeJsonValue

import controllers.routes

object form {

  def apply(
    form: Form[_],
    reqUser: Option[lila.user.User],
    reqTitle: Option[String],
    reqMod: Boolean,
    canMessage: Boolean,
    oldEnough: Boolean
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.composeMessage.txt(),
      moreCss = cssTag("message"),
      moreJs = jsTag("message.js")
    ) {
        main(cls := "message-new box box-pad page-small")(
          h1(trans.composeMessage()),
          reqUser.ifFalse(canMessage).map { u =>
            frag(
              br, br, hr, br,
              p("Sorry, ", u.username, " doesn't accept new messages.")
            )
          } getOrElse {
            if (!oldEnough) frag(
              br, br, hr, br,
              p("Sorry, you cannot start conversations yet.")
            )
            else st.form(
              cls := "form3",
              action := s"${routes.Message.create()}${reqUser.??(u => "?username=" + u.username)}",
              method := "post"
            )(
                form3.group(form("username"), trans.recipient()) { f =>
                  reqUser map { user =>
                    frag(
                      userLink(user),
                      form3.hidden(f)
                    )
                  } getOrElse input(
                    cls := "form-control user-autocomplete",
                    required,
                    name := f.name,
                    id := form3.id(f),
                    value := f.value,
                    autofocus,
                    dataTag := "span"
                  )
                },
                isGranted(_.ModMessage) option frag(
                  form3.checkbox(form("mod"), frag("Send as mod")),
                  form3.group(form("preset"), frag("Preset")) { form3.select(_, Nil) },
                  embedJsUnsafe(s"""lichess_mod_presets=${safeJsonValue(lila.message.ModPreset.asJson)}""")
                ),
                form3.group(form("subject"), trans.subject()) { f =>
                  input(
                    cls := "form-control",
                    required,
                    minlength := 3,
                    maxlength := 100,
                    name := f.name,
                    id := form3.id(f),
                    value := f.value.filter(_.nonEmpty).orElse(reqTitle),
                    reqUser.isDefined option autofocus
                  )
                },
                form3.group(form("text"), frag("Message"), klass = "message-text") { f =>
                  form3.textarea(f)(required)
                },
                form3.actions(
                  a(cls := "cancel", href := routes.Message.inbox())(trans.cancel()),
                  button(cls := "button text", dataIcon := "E", tpe := "submit")(trans.send())
                )
              )
          }
        )
      }
}

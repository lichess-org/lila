package views.html
package account

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

import controllers.routes

object kid:

  def apply(u: lila.user.User, form: play.api.data.Form[?], managed: Boolean)(using PageContext) =
    account.layout(
      title = s"${u.username} - ${trans.kidMode.txt()}",
      active = "kid"
    ) {
      div(cls := "account box box-pad")(
        h1(cls := "box__top")(trans.kidMode()),
        standardFlash,
        p(trans.kidModeExplanation()),
        br,
        br,
        br,
        if managed then p(trans.askYourChessTeacherAboutLiftingKidMode())
        else
          postForm(cls := "form3", action := s"${routes.Account.kidPost}?v=${!u.kid}")(
            form3.passwordModified(form("passwd"), trans.password())(autofocus, autocomplete := "off"),
            submitButton(
              cls := List(
                "button"     -> true,
                "button-red" -> u.kid
              )
            )(if u.kid then trans.disableKidMode.txt() else trans.enableKidMode.txt())
          )
        ,
        br,
        br,
        p(trans.inKidModeTheLichessLogoGetsIconX(span(cls := "kiddo", title := trans.kidMode.txt())(":)")))
      )
    }

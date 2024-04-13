package views.html
package account

import controllers.routes

import lila.app.templating.Environment.{ *, given }
import lila.web.ui.ScalatagsTemplate.{ *, given }

object kid:

  def apply(u: User, form: play.api.data.Form[?], managed: Boolean)(using PageContext) =
    account.layout(
      title = s"${u.username} - ${trans.site.kidMode.txt()}",
      active = "kid"
    ):
      div(cls := "box box-pad")(
        h1(cls := "box__top")(if u.kid then trans.site.kidModeIsEnabled() else trans.site.kidMode()),
        standardFlash,
        p(trans.site.kidModeExplanation()),
        br,
        br,
        br,
        if managed then p(trans.site.askYourChessTeacherAboutLiftingKidMode())
        else
          postForm(cls := "form3", action := s"${routes.Account.kidPost}?v=${!u.kid}")(
            form3.passwordModified(form("passwd"), trans.site.password())(autofocus, autocomplete := "off"),
            submitButton(
              cls := List(
                "button"     -> true,
                "button-red" -> u.kid
              )
            )(if u.kid then trans.site.disableKidMode.txt() else trans.site.enableKidMode.txt())
          )
        ,
        br,
        br,
        p(
          trans.site.inKidModeTheLichessLogoGetsIconX(
            span(cls := "kiddo", title := trans.site.kidMode.txt())(":)")
          )
        )
      )

package views.html
package account

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object kid {

  def apply(u: lidraughts.user.User)(implicit ctx: Context) = account.layout(
    title = s"${u.username} - ${trans.kidMode.txt()}",
    active = "kid"
  ) {
    div(cls := "content_box small_box high")(
      div(cls := "signup_box")(
        h1(cls := "lidraughts_title")(trans.kidMode.frag()),
        p(cls := "explanation")(trans.kidModeExplanation.frag()),
        br,
        br,
        br,
        st.form(action := s"${routes.Account.kidPost}?v=${!u.kid}", method := "POST")(
          input(tpe := "submit", cls := "submit button", value := (if (u.kid) { trans.disableKidMode.txt() } else { trans.enableKidMode.txt() }))
        ),
        br,
        br,
        p(trans.inKidModeTheLidraughtsLogoGetsIconX.frag(raw(s"""<span title="${trans.kidMode()}" class="kiddo">😊</span>""")))
      )
    )
  }
}

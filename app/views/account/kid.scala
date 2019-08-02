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
    div(cls := "account box box-pad")(
      h1(trans.kidMode()),
      p(trans.kidModeExplanation()),
      br,
      br,
      br,
      postForm(action := s"${routes.Account.kidPost}?v=${!u.kid}")(
        submitButton(cls := List(
          "button" -> true,
          "button-red" -> u.kid
        ))(if (u.kid) trans.disableKidMode.txt() else trans.enableKidMode.txt())
      ),
      br,
      br,
      p(trans.inKidModeTheLidraughtsLogoGetsIconX(span(cls := "kiddo", title := trans.kidMode.txt())(":)")))
    )
  }
}

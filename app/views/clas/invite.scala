package views.html.clas

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.clas.{ Clas, ClasInvite }

object invite {

  def show(
      c: Clas,
      invite: ClasInvite
  )(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = cssTag("clas"),
      title = c.name
    ) {
      main(cls := "page-small box box-pad page clas-invitation")(
        h1(c.name),
        p(c.desc),
        br,
        br,
        p("You have been invited by ", userIdLink(invite.created.by.some), "."),
        br,
        br,
        invite.accepted.map {
          case true  => flashMessage(cls := "flash-success")("You have accepted this invitation.")
          case false => flashMessage(cls := "flash-warning")("You have declined this invitation.")
        },
        invite.accepted.fold(true)(false.==) option
          postForm(cls := "form3", action := routes.Clas.invitationAccept(invite._id.value))(
            form3.actions(
              if (!invite.accepted.has(false))
                form3.submit(
                  trans.decline(),
                  nameValue = ("v" -> false.toString).some,
                  klass = "button-red button-fat",
                  icon = "".some
                )
              else p,
              form3.submit(
                trans.accept(),
                klass = "button-green button-fat",
                nameValue = ("v" -> true.toString).some
              )
            )
          )
      )
    }
}

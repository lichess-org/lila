package views.html.clas

import controllers.clas.routes.{ Clas as clasRoutes }

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.clas.{ Clas, ClasInvite }

object invite:

  def show(
      c: Clas,
      invite: ClasInvite
  )(using PageContext) =
    views.html.base.layout(
      moreCss = cssTag("clas"),
      title = c.name
    ) {
      main(cls := "page-small box box-pad page clas-invitation")(
        h1(cls := "box__top")(c.name),
        p(c.desc),
        br,
        br,
        p(trans.clas.youHaveBeenInvitedByX(userIdLink(invite.created.by.some))),
        br,
        br,
        invite.accepted.map {
          if _ then flashMessage("success")(trans.clas.youAcceptedThisInvitation())
          else flashMessage("warning")(trans.clas.youDeclinedThisInvitation())
        },
        invite.accepted.forall(false.==) option
          postForm(cls := "form3", action := clasRoutes.invitationAccept(invite._id.value))(
            form3.actions(
              if !invite.accepted.has(false) then
                form3.submit(
                  trans.decline(),
                  nameValue = ("v" -> false.toString).some,
                  icon = licon.X.some
                )(cls := "button-red button-fat")
              else p,
              form3.submit(
                trans.accept(),
                nameValue = ("v" -> true.toString).some
              )(cls := "button-green button-fat")
            )
          )
      )
    }

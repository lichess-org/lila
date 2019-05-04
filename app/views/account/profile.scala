package views.html
package account

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object profile {

  private val linksHelp = frag(
    "Such as Twitter, Facebook, GitHub, Chess.com, ...",
    br,
    "One URL per line."
  )

  def apply(u: lila.user.User, form: play.api.data.Form[_])(implicit ctx: Context) = account.layout(
    title = s"${u.username} - ${trans.editProfile.txt()}",
    active = "editProfile"
  ) {
    div(cls := "account box box-pad")(
      h1(trans.editProfile()),
      st.form(cls := "form3", action := routes.Account.profileApply, method := "POST")(
        div(cls := "form-group")(trans.allInformationIsPublicAndOptional()),
        form3.split(
          form3.group(form("country"), trans.country(), half = true) { f =>
            form3.select(f, lila.user.Countries.allPairs, default = "".some)
          },
          form3.group(form("location"), trans.location(), half = true)(form3.input(_))
        ),
        NotForKids {
          form3.group(form("bio"), trans.biography(), help = trans.biographyDescription().some) { f =>
            form3.textarea(f)(rows := 5)
          }
        },
        form3.split(
          form3.group(form("firstName"), trans.firstName(), half = true)(form3.input(_)),
          form3.group(form("lastName"), trans.lastName(), half = true)(form3.input(_))
        ),
        form3.split(
          List("fide", "uscf", "ecf").map { rn =>
            form3.group(form(s"${rn}Rating"), trans.xRating(rn.toUpperCase), help = trans.ifNoneLeaveEmpty().some, klass = "form-third")(form3.input(_, typ = "number"))
          }
        ),
        form3.group(form("links"), raw("Social media links "), help = Some(linksHelp)) { f =>
          form3.textarea(f)(rows := 5)
        },
        form3.action(form3.submit(trans.apply()))
      )
    )
  }
}

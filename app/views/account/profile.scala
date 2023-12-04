package views.html
package account

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import play.api.i18n.Lang

import controllers.routes

object profile:

  private def linksHelp()(using Lang) = frag(
    "Mastodon, Facebook, GitHub, Chess.com, ...",
    br,
    trans.oneUrlPerLine()
  )

  def apply(u: lila.user.User, form: play.api.data.Form[?])(using ctx: PageContext) =
    account.layout(
      title = s"${u.username} - ${trans.editProfile.txt()}",
      active = "editProfile"
    ):
      div(cls := "box box-pad")(
        h1(cls := "box__top")(trans.editProfile()),
        standardFlash,
        postForm(cls := "form3", action := routes.Account.profileApply)(
          div(cls := "form-group")(trans.allInformationIsPublicAndOptional()),
          form3.split(
            ctx.kid.no option
              form3
                .group(form("bio"), trans.biography(), half = true, help = trans.biographyDescription().some):
                  f => form3.textarea(f)(rows := 5)
            ,
            form3.group(form("flair"), "Flair", half = true): f =>
              frag(
                details(cls := "form-control emoji-details")(
                  summary(cls := "button button-metal button-no-upper")(
                    trans.setFlair(),
                    userSpan(u, withPowerTip = false, cssClass = "flair-container".some)
                  ),
                  form3.hidden(f, form("flair").value),
                  div(cls := "flair-picker")
                ),
                u.flair.isDefined option p(
                  button(
                    cls := "button button-red button-thin button-empty text emoji-remove"
                  )(trans.delete())
                ),
                p(cls := "form-help")(
                  a(
                    href     := s"${routes.Pref.form("display")}#showFlairs",
                    cls      := "text",
                    dataIcon := licon.InfoCircle
                  ):
                    trans.youCanHideFlair()
                )
              )
          ),
          form3.split(
            form3.group(form("flag"), trans.countryRegion(), half = true): f =>
              form3.select(f, lila.user.Flags.allPairs, default = "".some),
            form3.group(form("location"), trans.location(), half = true)(form3.input(_))
          ),
          form3.split(
            form3.group(form("firstName"), trans.firstName(), half = true)(form3.input(_)),
            form3.group(form("lastName"), trans.lastName(), half = true)(form3.input(_))
          ),
          form3.split(
            List("fide", "uscf", "ecf", "rcf", "cfc", "dsb").map: rn =>
              form3.group(
                form(s"${rn}Rating"),
                trans.xRating(rn.toUpperCase),
                help = trans.ifNoneLeaveEmpty().some,
                klass = "form-third"
              )(form3.input(_, typ = "number"))
          ),
          form3.group(form("links"), trans.socialMediaLinks(), help = Some(linksHelp())): f =>
            form3.textarea(f)(rows := 5),
          form3.action(form3.submit(trans.apply()))
        )
      )

package views.html
package account

import controllers.routes

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.{ *, given }

object profile:

  private def linksHelp()(using Translate) = frag(
    "Mastodon, Facebook, GitHub, Chess.com, ...",
    br,
    trans.site.oneUrlPerLine()
  )

  def apply(u: lila.user.User, form: play.api.data.Form[?])(using ctx: PageContext) =
    account.layout(
      title = s"${u.username} - ${trans.site.editProfile.txt()}",
      active = "editProfile"
    ):
      div(cls := "box box-pad")(
        h1(cls := "box__top")(trans.site.editProfile()),
        standardFlash,
        postForm(cls := "form3 dirty-alert", action := routes.Account.profileApply)(
          div(cls := "form-group")(trans.site.allInformationIsPublicAndOptional()),
          form3.split(
            ctx.kid.no.option(
              form3
                .group(
                  form("bio"),
                  trans.site.biography(),
                  half = true,
                  help = trans.site.biographyDescription().some
                ): f =>
                  form3.textarea(f)(rows := 5)
            ),
            form3.flairPickerGroup(form("flair"), u.flair, label = trans.site.setFlair())(
              userSpan(u, withPowerTip = false, cssClass = "flair-container".some)
            ):
              p(cls := "form-help"):
                a(
                  href     := s"${routes.Pref.form("display")}#showFlairs",
                  cls      := "text",
                  dataIcon := licon.InfoCircle
                ):
                  trans.site.youCanHideFlair()
          ),
          form3.split(
            form3.group(form("flag"), trans.site.countryRegion(), half = true): f =>
              form3.select(f, lila.user.Flags.allPairs, default = "".some),
            form3.group(form("location"), trans.site.location(), half = true)(form3.input(_))
          ),
          form3.split(
            form3.group(form("firstName"), trans.site.firstName(), half = true)(form3.input(_)),
            form3.group(form("lastName"), trans.site.lastName(), half = true)(form3.input(_))
          ),
          form3.split(
            List("fide", "uscf", "ecf", "rcf", "cfc", "dsb").map: rn =>
              form3.group(
                form(s"${rn}Rating"),
                trans.site.xRating(rn.toUpperCase),
                help = trans.site.ifNoneLeaveEmpty().some,
                klass = "form-third"
              )(form3.input(_, typ = "number"))
          ),
          form3.group(form("links"), trans.site.socialMediaLinks(), help = Some(linksHelp())): f =>
            form3.textarea(f)(rows := 5),
          form3.action(form3.submit(trans.site.apply()))
        )
      )

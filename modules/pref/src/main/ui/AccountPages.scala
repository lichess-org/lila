package lila.pref
package ui

import play.api.data.Form

import lila.ui.*
import ScalatagsTemplate.{ *, given }

final class AccountPages(helpers: Helpers, flagApi: lila.core.user.FlagApi):
  import helpers.{ *, given }
  import trans.{ settings as trs }

  def close(form: Form[?], managed: Boolean)(using Context)(using me: Me) =
    div(cls := "box box-pad")(
      boxTop(h1(cls := "text", dataIcon := Icon.CautionCircle)(trs.closeAccount())),
      if managed then p(trs.managedAccountCannotBeClosed())
      else
        postForm(cls := "form3", action := routes.Account.closeConfirm)(
          div(cls := "form-group")(trs.closeAccountExplanation()),
          div(cls := "form-group")(trs.cantOpenSimilarAccount()),
          form3.passwordModified(form("passwd"), trans.site.password())(autofocus, autocomplete := "off"),
          form3.actions(
            frag(
              a(href := routes.User.show(me.username))(trs.changedMindDoNotCloseAccount()),
              form3.submit(
                trs.closeAccount(),
                icon = Icon.CautionCircle.some,
                confirm = trs.closingIsDefinitive.txt().some
              )(cls := "button-red")
            )
          )
        )
    )

  private def linksHelp()(using Translate) = frag(
    "Mastodon, Facebook, GitHub, Chess.com, ...",
    br,
    trans.site.oneUrlPerLine()
  )

  private lazy val flagPairs = flagApi.all.map: c =>
    c.code -> c.name

  def profile(u: User, form: Form[?])(using ctx: Context) =
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
                dataIcon := Icon.InfoCircle
              ):
                trans.site.youCanHideFlair()
        ),
        form3.split(
          form3.group(form("flag"), trans.site.countryRegion(), half = true): f =>
            form3.select(f, flagPairs, default = "".some),
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

  def network(cfRouting: Option[Boolean], isUsingAltSocket: Boolean)(using ctx: Context) =
    val usingCloudflare = cfRouting.getOrElse(isUsingAltSocket)
    div(cls := "box box-pad")(
      h1(cls := "box__top")("Network"),
      br,
      if usingCloudflare then
        frag(
          flashMessage("warning")("You are currently using Content Delivery Network (CDN) routing."),
          p("This feature is experimental but may improve reliability in some regions.")
        )
      else p("If you have frequent disconnects, Content Delivery Network (CDN) routing may improve things."),
      br,
      st.section(a(href := "#routing")(h2(id := "routing")("Network Routing")))(
        st.group(cls := "radio"):
          List(("Use direct routing", false), ("Use CDN routing", true)).map: (key, value) =>
            div(
              a((value != usingCloudflare).option(href := routes.Account.network(value.some)))(
                label((value == usingCloudflare).option(cls := "active-soft"))(key)
              )
            )
      ),
      br,
      br,
      cfRouting.nonEmpty.option(
        p(cls := "saved text", dataIcon := Icon.Checkmark)(
          trans.preferences.yourPreferencesHaveBeenSaved()
        )
      )
    )

  def kid(u: User, form: Form[?], managed: Boolean)(using Context) =
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

  def password(form: Form[?])(using Context) =
    div(cls := "box box-pad")(
      h1(cls := "box__top")(trans.site.changePassword()),
      standardFlash | flashMessage("warning")(trans.site.passwordSuggestion()),
      postForm(cls := "form3", action := routes.Account.passwdApply)(
        form3.passwordModified(form("oldPasswd"), trans.site.currentPassword())(
          autofocus,
          autocomplete := "current-password"
        ),
        form3.passwordModified(form("newPasswd1"), trans.site.newPassword())(
          autocomplete := "new-password"
        ),
        form3.passwordComplexityMeter(trans.site.newPasswordStrength()),
        form3.passwordModified(form("newPasswd2"), trans.site.newPasswordAgain())(
          autocomplete := "new-password"
        ),
        form3.globalError(form),
        form3.action(form3.submit(trans.site.apply()))
      )
    )

  def username(u: User, form: Form[?])(using Context) =
    div(cls := "box box-pad")(
      h1(cls := "box__top")(trans.site.changeUsername()),
      standardFlash,
      postForm(cls := "form3", action := routes.Account.usernameApply)(
        form3.globalError(form),
        form3.group(
          form("username"),
          trans.site.username(),
          help = trans.site.changeUsernameDescription().some
        )(
          form3.input(_)(autofocus, required, autocomplete := "username")
        ),
        form3.action(form3.submit(trans.site.apply()))
      )
    )

  def email(form: Form[?])(using Context) =
    div(cls := "box box-pad")(
      h1(cls := "box__top")(trans.site.changeEmail()),
      standardFlash | flashMessage("warning")(trans.site.emailSuggestion()),
      postForm(cls := "form3", action := routes.Account.emailApply)(
        form3.passwordModified(form("passwd"), trans.site.password())(autofocus),
        form3.group(form("email"), trans.site.email())(form3.input(_, typ = "email")(required)),
        form3.action(form3.submit(trans.site.apply()))
      )
    )

  def data(u: User)(using Context) =
    div(cls := "security personal-data box box-pad")(
      h1(cls := "box__top")("My personal data"),
      div(cls := "personal-data__header")(
        p("Here is all personal information Lichess has about ", userLink(u)),
        a(cls := "button", href := s"${routes.Account.data}?user=${u.id}&text=1", downloadAttr):
          trans.site.download()
      )
    )

  object reopen:

    def form(form: lila.core.security.HcaptchaForm[?], error: Option[String] = None)(using
        ctx: Context
    ) =
      main(cls := "page-small box box-pad")(
        h1(cls := "box__top")(trans.site.reopenYourAccount()),
        p(trans.site.closedAccountChangedMind()),
        p(strong(trans.site.onlyWorksOnce())),
        p(trans.site.cantDoThisTwice()),
        hr,
        postForm(cls := "form3", action := routes.Account.reopenApply)(
          error.map: err =>
            p(cls := "error")(strong(err)),
          form3.group(form("username"), trans.site.username())(form3.input(_)(autofocus)),
          form3
            .group(form("email"), trans.site.email(), help = trans.site.emailAssociatedToaccount().some)(
              form3.input(_, typ = "email")
            ),
          lila.ui.bits.hcaptcha(form),
          form3.action(form3.submit(trans.site.emailMeALink()))
        )
      )

    def sent(using Context) =
      main(cls := "page-small box box-pad")(
        boxTop(h1(cls := "is-green text", dataIcon := Icon.Checkmark)(trans.site.checkYourEmail())),
        p(trans.site.sentEmailWithLink()),
        p(trans.site.ifYouDoNotSeeTheEmailCheckOtherPlaces())
      )

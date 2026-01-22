package lila.pref
package ui

import play.api.data.Form

import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class AccountPages(helpers: Helpers, ui: AccountUi, flagApi: lila.core.user.FlagApi):
  import helpers.{ *, given }
  import trans.settings as trs
  import ui.AccountPage

  private def myUsernamePasswordFields(form: Form[?])(using Context) =
    form3.split(
      form3.group(form("username"), trans.site.username(), half = true)(
        form3.input(_)(required, autocomplete := "off")
      ),
      form3.passwordModified(form("passwd"), trans.site.password(), half = true)()
    )

  def close(form: Form[?], managed: Boolean)(using Context)(using me: Me) =
    AccountPage(s"${me.username} - ${trans.settings.closeAccount.txt()}", "close"):
      div(cls := "box box-pad")(
        boxTop(h1(cls := "text", dataIcon := Icon.CautionCircle)(trs.closeAccount())),
        if managed then p(trs.managedAccountCannotBeClosed())
        else
          postForm(cls := "form3", action := routes.Account.closeConfirm)(
            div(cls := "form-group")(h2("We're sorry to see you go.")),
            div(cls := "form-group")(trs.closeAccountAreYouSure()),
            div(cls := "form-group")(trs.cantOpenSimilarAccount()),
            myUsernamePasswordFields(form),
            form3.checkboxGroup(
              form("forever"),
              raw("Forever close: make it impossible to reopen"),
              help = raw(
                "Prevent reopening the account later. If you check this box, even administrators will be unable to reopen your account at your request."
              ).some
            ),
            form3.actions(
              frag(
                a(href := routes.User.show(me.username))(trs.cancelKeepAccount()),
                form3.submit(
                  trs.closeAccount(),
                  icon = Icon.CautionCircle.some,
                  confirm = trs.closeAccountAreYouSure.txt().some
                )(cls := "button-red")
              )
            )
          )
      )

  def delete(form: Form[?], managed: Boolean)(using Context)(using me: Me) =
    AccountPage(s"${me.username} - Delete your account", "delete"):
      div(cls := "box box-pad")(
        boxTop(h1(cls := "text", dataIcon := Icon.CautionCircle)("Delete your account")),
        if managed then p(trs.managedAccountCannotBeClosed())
        else
          postForm(cls := "form3", action := routes.Account.deleteConfirm)(
            div(cls := "form-group")(h2("We're sorry to see you go.")),
            div(cls := "form-group")(
              "Once you delete your account, it’s removed from Lichess and our administrators won’t be able to bring it back for you."
            ),
            div(cls := "form-group")(trs.cantOpenSimilarAccount()),
            div(cls := "form-group")(
              "Would you like to ",
              a(href := routes.Account.close)("close your account"),
              " instead?"
            ),
            myUsernamePasswordFields(form),
            form3.checkbox(form("understand"), "I understand that deleted accounts aren't recoverable"),
            form3.errors(form("understand")),
            me.marks.dirty.option:
              div(cls := "form-group")(
                h2("Note about GDPR erasure and TOS violations"),
                p(
                  "One of the rights GDPR grants to European citizens is the right to erasure of their personal information, also known as the \"right to be forgotten\"."
                ),
                p(
                  "Lichess generally complies with these requests from citizens of any country, because individuals should have control of their data against organisations. However, in certain cases where accounts broke our ",
                  a(href := routes.Cms.tos)("Terms of Service"),
                  ", we cannot comply with those requests."
                ),
                p(
                  "That is because the GDPR allows for exceptions in certain cases, and one of those is where an organisation's overriding legitimate interests would be compromised by erasing the data. In short, by deleting your data, it would make it harder for us to keep Lichess safe and secure from people who have broken our rules."
                ),
                p(
                  "When you delete your account, your personal data will be hidden from the public, and only accessible by admins."
                ),
                p(
                  "For more information on the data we process and how we use it, please refer to our ",
                  a(href := routes.Cms.menuPage(lila.core.id.CmsPageKey("privacy")))("Privacy Policy"),
                  "."
                ),
                p(
                  "If you continue to disagree with our decision you have the right to make a complaint to ",
                  a(href := "https://www.cnil.fr/en/contact-cnil")("CNIL"),
                  ". Your statutory rights are also unaffected by our decision."
                )
              )
            ,
            form3.actions(
              frag(
                a(href := routes.User.show(me.username))(trs.cancelKeepAccount()),
                form3.submit(
                  "Delete my account",
                  icon = Icon.CautionCircle.some,
                  confirm = "Deleting is definitive, there is no going back. Are you sure?".some
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
    AccountPage(s"${u.username} - ${trans.site.editProfile.txt()}", "editProfile"):
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
            form3.flairPickerGroup(form("flair"), u.flair):
              p(cls := "form-help"):
                a(
                  href := s"${routes.Pref.form("display")}#showFlairs",
                  cls := "text",
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
            form3.group(form("realName"), trans.site.realName(), half = true)(form3.input(_))
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
          ctx.kid.no.option(
            form3.group(form("links"), trans.site.socialMediaLinks(), help = Some(linksHelp())): f =>
              form3.textarea(f)(rows := 5)
          ),
          form3.action(form3.submit(trans.site.apply()))
        )
      )

  def kid(u: User, form: Form[?], managed: Boolean, cms: Option[Html])(using Context) =
    AccountPage(s"${u.username} - ${trans.site.kidMode.txt()}", "kid")
      .css("bits.page"):
        frag(
          div(cls := "box box-pad")(
            h1(cls := "box__top")(if u.kid.yes then trans.site.kidModeIsEnabled() else trans.site.kidMode()),
            standardFlash,
            p(trans.site.kidModeExplanation()),
            br,
            br,
            br,
            if managed then p(trans.site.askYourChessTeacherAboutLiftingKidMode())
            else
              postForm(cls := "form3", action := s"${routes.Account.kidPost}?v=${!u.kid}")(
                form3
                  .passwordModified(form("passwd"), trans.site.password())(autofocus, autocomplete := "off"),
                submitButton(
                  cls := List(
                    "button" -> true,
                    "button-red" -> u.kid.yes
                  )
                )(if u.kid.yes then trans.site.disableKidMode.txt() else trans.site.enableKidMode.txt())
              )
            ,
            br,
            br,
            p(
              trans.site.inKidModeTheLichessLogoGetsIconX(
                span(cls := "kiddo", title := trans.site.kidMode.txt())(":)")
              )
            )
          ),
          cms.map: content =>
            frag(br, div(cls := "box box-pad page")(div(cls := "body expand-text")(content)))
        )

  def password(form: Form[?])(using Context) =
    AccountPage(trans.site.changePassword.txt(), "password").js(esmInit("bits.passwordComplexity")):
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
    AccountPage(s"${u.username} - ${trans.site.editProfile.txt()}", "username"):
      div(cls := "box box-pad")(
        h1(cls := "box__top")(trans.site.changeUsername()),
        standardFlash,
        postForm(cls := "form3", action := routes.Account.usernameApply)(
          form3.globalError(form),
          form3.group(
            form("username"),
            trans.site.username(),
            help = trans.site.changeUsernameDescription().some
          )(form3.input(_)(autofocus, required, autocomplete := "username")),
          form3.action(form3.submit(trans.site.apply()))
        )
      )

  def email(form: Form[?])(using Context) =
    AccountPage(trans.site.changeEmail.txt(), "email"):
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
    AccountPage(s"${u.username} - personal data", "security"):
      div(cls := "security personal-data box box-pad")(
        h1(cls := "box__top")("My personal data"),
        div(cls := "personal-data__header")(
          p("Here is all personal information Lichess has about ", userLink(u)),
          a(cls := "button", href := s"${routes.Account.data}?user=${u.id}&text=1", downloadAttr):
            trans.site.download()
        )
      )

  object reopen:

    def form(form: lila.core.security.HcaptchaForm[?], error: Option[String] = None)(using ctx: Context) =
      Page(trans.site.reopenYourAccount.txt())
        .css("bits.auth")
        .js(hcaptchaScript(form))
        .csp(_.withHcaptcha):
          main(cls := "page-small box box-pad")(
            h1(cls := "box__top")(trans.site.reopenYourAccount()),
            p(trans.site.reopenYourAccountDescription()),
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
      Page(trans.site.reopenYourAccount.txt()):
        main(cls := "page-small box box-pad")(
          boxTop(h1(cls := "is-green text", dataIcon := Icon.Checkmark)(trans.site.checkYourEmail())),
          p(trans.site.sentEmailWithLink()),
          p(trans.site.ifYouDoNotSeeTheEmailCheckOtherPlaces())
        )

package views.account

import play.api.data.Form

import lila.app.templating.Environment.{ *, given }

private lazy val ui = lila.pref.ui.AccountPages(helpers, flagApi)

def close(form: Form[?], managed: Boolean)(using PageContext)(using me: Me) =
  layout(
    title = s"${me.username} - ${trans.settings.closeAccount.txt()}",
    active = "close"
  )(ui.close(form, managed))

def profile(u: User, form: Form[?])(using ctx: PageContext) =
  layout(
    title = s"${u.username} - ${trans.site.editProfile.txt()}",
    active = "editProfile"
  )(ui.profile(u, form))

def network(cfRouting: Option[Boolean])(using ctx: PageContext) =
  layout(
    title = "Network",
    active = "network"
  )(ui.network(cfRouting, ctx.pref.isUsingAltSocket))

def kid(u: User, form: Form[?], managed: Boolean)(using PageContext) =
  layout(
    title = s"${u.username} - ${trans.site.kidMode.txt()}",
    active = "kid"
  )(ui.kid(u, form, managed))

def passwd(form: Form[?])(using PageContext) =
  layout(
    title = trans.site.changePassword.txt(),
    active = "password",
    modules = jsModuleInit("bits.passwordComplexity")
  )(ui.password(form))

def username(u: User, form: Form[?])(using PageContext) =
  layout(
    title = s"${u.username} - ${trans.site.editProfile.txt()}",
    active = "username"
  )(ui.username(u, form))

def email(form: Form[?])(using PageContext) =
  layout(
    title = trans.site.changeEmail.txt(),
    active = "email"
  )(ui.email(form))

def data(u: User)(using PageContext) =
  layout(title = s"${u.username} - personal data", active = "security"):
    ui.data(u)

object reopen:

  def form(form: lila.core.security.HcaptchaForm[?], error: Option[String] = None)(using
      ctx: PageContext
  ) =
    views.base.layout(
      title = trans.site.reopenYourAccount.txt(),
      moreCss = cssTag("auth"),
      moreJs = hcaptchaScript(form),
      csp = defaultCsp.withHcaptcha.some
    )(ui.reopen.form(form, error))

  def sent(using PageContext) =
    views.base.layout(title = trans.site.reopenYourAccount.txt()):
      ui.reopen.sent
end reopen

private lazy val securityUi = lila.security.ui.AccountSecurity(helpers)
def security(
    u: User,
    sessions: List[lila.security.LocatedSession],
    curSessionId: String,
    clients: List[lila.oauth.AccessTokenApi.Client],
    personalAccessTokens: Int
)(using PageContext) =
  layout(title = s"${u.username} - ${trans.site.security.txt()}", active = "security"):
    securityUi(u, sessions, curSessionId, clients, personalAccessTokens)

private lazy val prefUi = lila.pref.ui.AccountPref(helpers, prefHelper, bits)
def pref(u: User, form: Form[?], categ: lila.pref.PrefCateg)(using PageContext) =
  layout(
    title = s"${bits.categName(categ)} - ${u.username} - ${trans.preferences.preferences.txt()}",
    active = categ.slug
  )(prefUi(u, form, categ))

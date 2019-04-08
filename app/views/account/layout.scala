package views.html
package account

import play.twirl.api.Html

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object layout {

  def apply(
    title: String,
    active: String,
    evenMoreCss: Html = emptyHtml,
    evenMoreJs: Html = emptyHtml
  )(body: Html)(implicit ctx: Context) = views.html.base.layout(
    title = title,
    moreCss = frag(responsiveCssTag("account"), evenMoreCss),
    responsive = true,
    moreJs = frag(jsTag("account.js"), evenMoreJs)
  ) {
      def activeCls(c: String) = cls := active.activeO(c)
      main(cls := "account page-menu")(
        st.nav(cls := "page-menu__menu subnav")(
          lila.pref.PrefCateg.all.map { categ =>
            a(activeCls(categ.slug), href := routes.Pref.form(categ.slug))(
              bits.categName(categ)
            )
          },
          a(activeCls("kid"), href := routes.Account.kid())(
            trans.kidMode.frag()
          ),
          div(cls := "sep"),
          a(activeCls("editProfile"), href := routes.Account.profile())(
            trans.editProfile.frag()
          ),
          isGranted(_.Coach) option a(activeCls("coach"), href := routes.Coach.edit)("Coach profile"),
          div(cls := "sep"),
          a(activeCls("password"), href := routes.Account.passwd())(
            trans.changePassword.frag()
          ),
          a(activeCls("email"), href := routes.Account.email())(
            trans.changeEmail.frag()
          ),
          a(activeCls("username"), href := routes.Account.username())(
            trans.changeUsername.frag()
          ),
          a(activeCls("twofactor"), href := routes.Account.twoFactor())(
            "Two-factor authentication"
          ),
          a(activeCls("security"), href := routes.Account.security())(
            trans.security.frag()
          ),
          div(cls := "sep"),
          a(href := routes.Plan.index)("Patron"),
          div(cls := "sep"),
          a(activeCls("oauth.token"), href := routes.OAuthToken.index)(
            "API Access tokens"
          ),
          ctx.noBot option a(activeCls("oauth.app"), href := routes.OAuthApp.index)("OAuth Apps"),
          div(cls := "sep"),
          a(activeCls("close"), href := routes.Account.close())(
            trans.closeAccount.frag()
          )
        ),
        div(cls := "page-menu__content")(body)
      )
    }
}

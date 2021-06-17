package views.html.account

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object layout {

  def apply(
      title: String,
      active: String,
      evenMoreCss: Frag = emptyFrag,
      evenMoreJs: Frag = emptyFrag
  )(body: Frag)(implicit ctx: Context): Frag =
    views.html.base.layout(
      title = title,
      moreCss = frag(cssTag("account"), evenMoreCss),
      moreJs = frag(jsModule("account"), evenMoreJs)
    ) {
      def activeCls(c: String) = cls := active.activeO(c)
      main(cls := "account page-menu")(
        ctx.me.exists(_.enabled) option st.nav(cls := "page-menu__menu subnav")(
          lila.pref.PrefCateg.all.map { categ =>
            a(activeCls(categ.slug), href := routes.Pref.form(categ.slug))(
              bits.categName(categ)
            )
          },
          a(activeCls("kid"), href := routes.Account.kid)(
            trans.kidMode()
          ),
          div(cls := "sep"),
          a(activeCls("editProfile"), href := routes.Account.profile)(
            trans.editProfile()
          ),
          a(activeCls("username"), href := routes.Account.username)(
            trans.changeUsername()
          ),
          isGranted(_.Coach) option a(activeCls("coach"), href := routes.Coach.edit)(
            trans.coach.lichessCoach()
          ),
          div(cls := "sep"),
          a(activeCls("password"), href := routes.Account.passwd)(
            trans.changePassword()
          ),
          a(activeCls("email"), href := routes.Account.email)(
            trans.changeEmail()
          ),
          a(activeCls("twofactor"), href := routes.Account.twoFactor)(
            trans.tfa.twoFactorAuth()
          ),
          a(activeCls("security"), href := routes.Account.security)(
            trans.security()
          ),
          div(cls := "sep"),
          a(href := routes.Plan.index)(trans.patron.lichessPatron()),
          div(cls := "sep"),
          a(activeCls("oauth.token"), href := routes.OAuthToken.index)(
            "API Access tokens"
          ),
          ctx.noBot option a(activeCls("oauth.app"), href := routes.OAuthApp.index)("OAuth Apps"),
          ctx.noBot option a(href := routes.DgtCtrl.index)("DGT board"),
          div(cls := "sep"),
          a(activeCls("close"), href := routes.Account.close)(
            trans.settings.closeAccount()
          )
        ),
        div(cls := "page-menu__content")(body)
      )
    }
}

package views.html.account

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

import controllers.routes

object layout:

  def apply(
      title: String,
      active: String,
      evenMoreCss: Frag = emptyFrag,
      evenMoreJs: Frag = emptyFrag
  )(body: Frag)(using ctx: PageContext): Frag =
    views.html.base.layout(
      title = title,
      moreCss = frag(cssTag("account"), evenMoreCss),
      moreJs = frag(jsModule("account"), evenMoreJs)
    ):
      def activeCls(c: String) = cls := active.activeO(c)
      main(cls := "account page-menu")(
        ctx.me.exists(_.enabled.yes) option views.html.site.bits.pageMenuSubnav(
          a(activeCls("editProfile"), href := routes.Account.profile)(
            trans.editProfile()
          ),
          div(cls := "sep"),
          lila.pref.PrefCateg.values.map: categ =>
            a(activeCls(categ.slug), href := routes.Pref.form(categ.slug))(
              bits.categName(categ)
            ),
          a(activeCls("notification"), href := routes.Pref.form("notification"))(
            trans.notifications()
          ),
          a(activeCls("kid"), href := routes.Account.kid)(
            trans.kidMode()
          ),
          div(cls := "sep"),
          a(activeCls("username"), href := routes.Account.username)(
            trans.changeUsername()
          ),
          isGranted(_.Coach) option a(activeCls("coach"), href := routes.Coach.edit)(
            trans.coach.lichessCoach()
          ),
          a(activeCls("password"), href := routes.Account.passwd)(
            trans.changePassword()
          ),
          a(activeCls("email"), href := routes.Account.email)(
            trans.changeEmail()
          ),
          a(activeCls("twofactor"), href := routes.Account.twoFactor)(
            trans.tfa.twoFactorAuth()
          ),
          a(activeCls("oauth.token"), href := routes.OAuthToken.index)(
            trans.oauthScope.apiAccessTokens()
          ),
          a(activeCls("security"), href := routes.Account.security)(
            trans.security()
          ),
          div(cls := "sep"),
          a(activeCls("network"), href := routes.Account.network(none))(
            "Network"
          ),
          ctx.noBot option a(href := routes.DgtCtrl.index)(
            trans.dgt.dgtBoard()
          ),
          div(cls := "sep"),
          a(activeCls("close"), href := routes.Account.close)(
            trans.settings.closeAccount()
          )
        ),
        div(cls := "page-menu__content")(body)
      )

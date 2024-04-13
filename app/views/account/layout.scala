package views.html.account

import controllers.routes

import lila.app.templating.Environment.{ *, given }
import lila.web.ui.ScalatagsTemplate.{ *, given }

object layout:

  def apply(
      title: String,
      active: String,
      evenMoreCss: Frag = emptyFrag,
      evenMoreJs: Frag = emptyFrag,
      modules: EsmList = Nil
  )(body: Frag)(using ctx: PageContext): Frag =
    views.html.base.layout(
      title = title,
      moreCss = frag(cssTag("account"), evenMoreCss),
      moreJs = evenMoreJs,
      modules = modules
    ):
      def activeCls(c: String) = cls := active.activeO(c)
      main(cls := "account page-menu")(
        ctx.me
          .exists(_.enabled.yes)
          .option(
            views.html.site.bits.pageMenuSubnav(
              a(activeCls("editProfile"), href := routes.Account.profile)(
                trans.site.editProfile()
              ),
              div(cls := "sep"),
              lila.pref.PrefCateg.values.map: categ =>
                a(activeCls(categ.slug), href := routes.Pref.form(categ.slug))(
                  bits.categName(categ)
                ),
              a(activeCls("notification"), href := routes.Pref.form("notification"))(
                trans.site.notifications()
              ),
              a(activeCls("kid"), href := routes.Account.kid)(
                trans.site.kidMode()
              ),
              div(cls := "sep"),
              a(activeCls("username"), href := routes.Account.username)(
                trans.site.changeUsername()
              ),
              isGranted(_.Coach).option(
                a(activeCls("coach"), href := routes.Coach.edit)(
                  trans.coach.lichessCoach()
                )
              ),
              a(activeCls("password"), href := routes.Account.passwd)(
                trans.site.changePassword()
              ),
              a(activeCls("email"), href := routes.Account.email)(
                trans.site.changeEmail()
              ),
              a(activeCls("twofactor"), href := routes.Account.twoFactor)(
                trans.tfa.twoFactorAuth()
              ),
              a(activeCls("oauth.token"), href := routes.OAuthToken.index)(
                trans.oauthScope.apiAccessTokens()
              ),
              a(activeCls("security"), href := routes.Account.security)(
                trans.site.security()
              ),
              div(cls := "sep"),
              a(activeCls("network"), href := routes.Account.network(none))(
                "Network"
              ),
              ctx.noBot.option(
                a(href := routes.DgtCtrl.index)(
                  trans.dgt.dgtBoard()
                )
              ),
              div(cls := "sep"),
              a(activeCls("close"), href := routes.Account.close)(
                trans.settings.closeAccount()
              )
            )
          ),
        div(cls := "page-menu__content")(body)
      )

package lila.web
package ui

import play.api.mvc.RequestHeader

import lila.core.i18n.{ I18nKey as trans, Translate }
import lila.ui.*

import ScalatagsTemplate.{ *, given }

def mobileRedirect(using req: RequestHeader)(using Translate) =
  val callbackUrl = "org.lichess.mobile://login-callback" + req.rawQueryString.nonEmptyOption.so("?" + _)
  Page(trans.app.returningToApp.txt()).i18n(_.app):
    main(cls := "page-small box box-pad")(
      boxTop(
        h1(cls := "text")(trans.app.returningToApp())
      ),
      p(trans.app.ifAppDoesNotOpenAutomatically(trans.app.openTheApp())),
      a(href := callbackUrl, cls := "button")(trans.app.openTheApp())
    )

def mobile(helpers: Helpers)(renderedCmsPage: Frag)(using Translate) =
  import helpers.*

  val appleStoreButton = raw:
    s"""
  <a class="store"
    href="${StaticContent.mobileIosUrl}">
    <img alt="${trans.app.downloadOnAppleAppStore.txt()}"
    width="172"
    height="50"
    src="${assetUrl("images/mobile/apple-store.svg")}" />
  </a>
  """

  val googlePlayButton = raw:
    s"""
  <a class="store"
    href="${StaticContent.mobileAndroidUrl}">
    <img alt="${trans.app.downloadOnGooglePlay.txt()}"
    width="172"
    height="50"
    src="${assetUrl("images/mobile/google-play.webp")}" />
  </a>
  """

  val fdroidButton = raw:
    s"""
  <a class="store"
    href="${StaticContent.mobileFdroidUrl}">
    <img alt="${trans.app.downloadOnFdroid.txt()}"
    width="172"
    height="50"
    src="${assetUrl("images/mobile/fdroid.svg")}" />
  </a>
  """

  Page(trans.app.lichessMobileApp.txt())
    .i18n(_.app)
    .js(Esm("bits.qrcode"))
    .css("bits.mobile")
    .hrefLangs(lila.ui.LangPath(routes.Main.app)):
      main(
        div(cls := "mobile page-small box box-pad")(
          h1(cls := "box__top")(trans.app.lichessMobileApp()),
          div(cls := "sides")(
            div(cls := "left-side")(
              div(cls := "stores")(
                googlePlayButton,
                fdroidButton,
                appleStoreButton
              ),
              renderedCmsPage,
              qrcode(routeUrl(routes.Main.redirectToAppStore), 300),
              div(
                trans.app.viewAllReleases(
                  a(href := "https://github.com/lichess-org/mobile/releases")(trans.app.allReleases())
                )
              )
            ),
            div(cls := "right-side")(
              a(href := routes.Main.redirectToAppStore):
                img(
                  widthA := "358",
                  heightA := "766",
                  cls := "mobile-playing",
                  src := assetUrl("images/mobile/lichess-mobile-screen.webp"),
                  alt := trans.app.lichessMobileScreen.txt()
                )
            )
          )
        )
      )

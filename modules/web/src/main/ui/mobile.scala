package lila.web
package ui

import lila.ui.*

import ScalatagsTemplate.{ *, given }

def mobile(helpers: Helpers)(renderedCmsPage: Frag) =
  import helpers.*

  val appleStoreButton = raw:
    s"""
  <a class="store"
    href="${StaticContent.mobileIosUrl}">
    <img alt="Download on the Apple App Store"
    width="172"
    height="50"
    src="${assetUrl("images/mobile/apple-store.svg")}" />
  </a>
  """

  val googlePlayButton = raw:
    s"""
  <a class="store"
    href="${StaticContent.mobileAndroidUrl}">
    <img alt="Android app on Google Play"
    width="172"
    height="50"
    src="${assetUrl("images/mobile/google-play.png")}" />
  </a>
  """

  Page("Mobile")
    .js(Esm("bits.qrcode"))
    .css("bits.mobile")
    .hrefLangs(lila.ui.LangPath(routes.Main.mobile)):
      main(
        div(cls := "mobile page-small box box-pad")(
          h1(cls := "box__top")("Lichess mobile app"),
          div(cls := "sides")(
            div(cls := "left-side")(
              div(cls := "stores")(
                googlePlayButton,
                appleStoreButton
              ),
              renderedCmsPage,
              qrcode(s"$netBaseUrl${routes.Main.redirectToAppStore}", 300),
              div("All releases ", a(href := "https://github.com/lichess-org/mobile/releases")("on GitHub"))
            ),
            div(cls := "right-side")(
              a(href := routes.Main.redirectToAppStore):
                img(
                  widthA := "358",
                  heightA := "766",
                  cls := "mobile-playing",
                  src := assetUrl("images/mobile/lichess-mobile-screen.png"),
                  alt := "Lichess mobile screen"
                )
            )
          )
        )
      )

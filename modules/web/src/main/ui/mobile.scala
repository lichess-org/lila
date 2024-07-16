package lila.web
package ui

import lila.ui.*

import ScalatagsTemplate.*

def mobile(helpers: Helpers)(renderedCmsPage: Frag)(using Context) =
  import helpers.{ *, given }
  Page("Mobile")
    .js(EsmInit("bits.qrcode"))
    .css("bits.mobile")
    .hrefLangs(lila.ui.LangPath(routes.Main.mobile)):
      main(
        div(cls := "mobile page-small box box-pad")(
          h1(cls := "box__top")(trans.site.playChessEverywhere()),
          div(cls := "sides")(
            div(cls := "left-side")(
              div(cls := "stores")(
                googlePlayButton,
                appleStoreButton
              ),
              div(cls := "apk")(renderedCmsPage),
              h2(trans.site.asFreeAsLichess()),
              ul(cls := "block")(
                li(trans.site.builtForTheLoveOfChessNotMoney()),
                li(trans.site.everybodyGetsAllFeaturesForFree()),
                li(trans.site.zeroAdvertisement()),
                li("Entirely ", a(href := "https://github.com/lichess-org/lichobile")("Open Source"))
              ),
              h2(trans.site.fullFeatured()),
              ul(cls := "block")(
                li(trans.site.phoneAndTablet()),
                li(trans.site.bulletBlitzClassical()),
                li(trans.site.correspondenceChess()),
                li(trans.site.onlineAndOfflinePlay()),
                li(trans.site.tournaments()),
                li(trans.site.puzzles()),
                li(trans.site.gameAnalysis()),
                li(trans.site.boardEditor()),
                li("Lichess TV"),
                li(trans.site.followAndChallengeFriends()),
                li(trans.site.availableInNbLanguages.pluralSame(80))
              )
            ),
            div(cls := "right-side")(
              img(
                widthA  := "437",
                heightA := "883",
                cls     := "mobile-playing",
                src     := assetUrl("images/mobile/lichesstv-mobile.png"),
                alt     := "Lichess TV on mobile"
              ),
              qrcode(s"$netBaseUrl${routes.Main.redirectToAppStore}")
            )
          )
        )
      )

private val appleStoreButton = raw:
  """
<a class="store"
  href="https://itunes.apple.com/us/app/lichess-free-online-chess/id968371784">
  <img alt="Download on the Apple App Store"
  width="172"
  height="50"
  src="https://upload.wikimedia.org/wikipedia/commons/3/3c/Download_on_the_App_Store_Badge.svg" />
</a>
"""

private val googlePlayButton = raw:
  """
<a class="store"
  href="https://play.google.com/store/apps/details?id=org.lichess.mobileapp">
  <img alt="Android app on Google Play"
  width="192"
  height="74"
  src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" />
</a>
"""

package views.html

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object mobile {

  def apply(apkDoc: io.prismic.Document, resolver: io.prismic.DocumentLinkResolver)(implicit ctx: Context) =
    views.html.base.layout(
      title = "Mobile",
      moreCss = cssTag("mobile")
    ) {
      main(
        div(cls := "mobile page-small box box-pad")(
          h1(trans.playShogiEverywhere()),
          div(cls := "sides")(
            div(cls := "left-side")(
              div(cls := "stores")(
                googlePlayButton,
                appleStoreButton
              ),
              div(cls := "apk")(
                raw(~apkDoc.getHtml("doc.content", resolver))
              ),
              h2(trans.asFreeAsLishogi()),
              ul(cls := "block")(
                li(trans.builtForTheLoveOfShogiNotMoney()),
                li(trans.everybodyGetsAllFeaturesForFree()),
                li(trans.zeroAdvertisement()),
                li("Entirely ", a(href := "https://github.com/veloce/lichobile")("Open Source"))
              ),
              h2(trans.fullFeatured()),
              ul(cls := "block")(
                li(trans.phoneAndTablet()),
                li(trans.bulletBlitzClassical()),
                li(trans.correspondenceShogi()),
                li(trans.onlineAndOfflinePlay()),
                li(trans.tournaments()),
                li(trans.puzzles()),
                li(trans.gameAnalysis()),
                li(trans.boardEditor()),
                li("Lishogi TV"),
                li(trans.followAndChallengeFriends()),
                li(trans.availableInNbLanguages.pluralSame(80))
              )
            ),
            div(cls := "right-side")(
              img(
                cls    := "nexus5-playing",
                width  := "268",
                height := "513",
                src    := staticUrl("images/mobile/nexus5-playing.png"),
                alt    := "Lishogi mobile on nexus 5"
              ),
              img(
                cls    := "qrcode",
                width  := "200",
                height := "200",
                src    := staticUrl("images/mobile/dynamic-qrcode.png"),
                alt    := "Download QR code"
              )
            )
          )
        )
      )
    }

  lazy val appleStoreButton = raw(
    """
<a class="store"
  href="">
  <img alt="Download on the Apple App Store"
  width="172"
  height="50"
  src="https://upload.wikimedia.org/wikipedia/commons/3/3c/Download_on_the_App_Store_Badge.svg" />
</a>
"""
  )

  lazy val googlePlayButton = raw(
    """
<a class="store"
  href="">
  <img alt="Android app on Google Play"
  width="192"
  height="74"
  src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" />
</a>
"""
  )
}

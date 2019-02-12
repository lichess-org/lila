package views.html

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object mobile {

  def apply(apkDoc: io.prismic.Document, resolver: io.prismic.DocumentLinkResolver)(implicit ctx: Context) = base.layout(
    title = "Mobile",
    moreCss = responsiveCssTag("mobile"),
    responsive = true
  ) {
      div(cls := "mobile box box-pad")(
        div(cls := "right-side")(
          img(cls := "nexus5-playing", width := "268", height := "513", src := staticUrl("images/mobile/nexus5-playing.png"), alt := "Lichess mobile on nexus 5"),
          img(cls := "qrcode", width := "200", height := "200", src := staticUrl("images/mobile/dynamic-qrcode.png"), alt := "Download QR code")
        ),
        div(cls := "left-side")(
          h1(
            trans.playChessEverywhere.frag(),
            googlePlayButton,
            appleStoreButton,
            div(cls := "apk")(
              ~apkDoc.getHtml("doc.content", resolver)
            ),
            h2(trans.asFreeAsLichess.frag()),
            ul(cls := "block")(
              li(trans.builtForTheLoveOfChessNotMoney.frag()),
              li(trans.everybodyGetsAllFeaturesForFree.frag()),
              li(trans.zeroAdvertisement.frag()),
              li("Entirely ,", a(href := "https://github.com/veloce/lichobile")("Open Source"))
            ),
            h2(trans.fullFeatured.frag()),
            ul(cls := "block")(
              li(trans.phoneAndTablet.frag()),
              li(trans.bulletBlitzClassical.frag()),
              li(trans.correspondenceChess.frag()),
              li(trans.onlineAndOfflinePlay.frag()),
              li(trans.tournaments.frag()),
              li(trans.puzzles.frag()),
              li(trans.gameAnalysis.frag()),
              li(trans.boardEditor.frag()),
              li("Lichess TV"),
              li(trans.followAndChallengeFriends.frag()),
              li(trans.availableInNbLanguages.pluralSameFrag(80))
            )
          )
        )
      )
    }

  lazy val appleStoreButton = raw("""
<a class="store"
  href="https://itunes.apple.com/us/app/lichess-free-online-chess/id968371784">
  <img alt="Download on the Apple App Store"
  width="172"
  height="50"
  src="https://upload.wikimedia.org/wikipedia/commons/3/3c/Download_on_the_App_Store_Badge.svg" />
</a>
""")

  lazy val googlePlayButton = raw("""
<a class="store"
  href="https://play.google.com/store/apps/details?id=org.lichess.mobileapp">
  <img alt="Android app on Google Play"
  width="192"
  height="74"
  src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" />
</a>
""")
}

package views.html

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object mobile {

  def apply()(implicit ctx: Context) = base.layout(
    title = "Mobile",
    moreCss = cssTag("mobile")
  ) {
      main(
        div(cls := "mobile page-small box box-pad")(
          h1(trans.playDraughtsEverywhere.frag()),
          div(cls := "sides")(
            div(cls := "left-side")(
              div(cls := "stores")(
                googlePlayButton,
                appleStoreButton
              ),
              h2(trans.asFreeAsLidraughts.frag()),
              ul(cls := "block")(
                li(trans.builtForTheLoveOfDraughtsNotMoney.frag()),
                li(trans.everybodyGetsAllFeaturesForFree.frag()),
                li(trans.zeroAdvertisement.frag()),
                li("Entirely ", a(href := "https://github.com/RoepStoep/lidrobile")("Open Source"))
              ),
              h2(trans.fullFeatured.frag()),
              ul(cls := "block")(
                li(trans.phoneAndTablet.frag()),
                li(trans.bulletBlitzClassical.frag()),
                li(trans.correspondenceDraughts.frag()),
                li(trans.onlineAndOfflinePlay.frag()),
                li(trans.tournaments.frag()),
                li(trans.puzzles.frag()),
                li(trans.gameAnalysis.frag()),
                li(trans.boardEditor.frag()),
                li("Lidraughts TV"),
                li(trans.followAndChallengeFriends.frag()),
                li(trans.availableInNbLanguages.pluralSameFrag(16))
              )
            ),
            div(cls := "right-side")(
              img(cls := "nexus5-playing", width := "268px", height := "536px", src := staticUrl("images/mobile/mobile-playing.jpg"), alt := "A game in progress on the Lidraughts mobile app")
            )
          )
        )
      )
    }

  lazy val appleStoreButton = raw("""
<a class="store"
  href="https://itunes.apple.com/us/app/lidraughts-online-draughts/id1485028698">
  <img alt="Download on the Apple App Store"
  width="172"
  height="50"
  src="https://upload.wikimedia.org/wikipedia/commons/3/3c/Download_on_the_App_Store_Badge.svg" />
</a>
""")

  lazy val googlePlayButton = raw("""
<a class="store"
  href="https://play.google.com/store/apps/details?id=org.lidraughts.mobileapp">
  <img alt="Android app on Google Play"
  width="192"
  height="74"
  src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" />
</a>
""")
}

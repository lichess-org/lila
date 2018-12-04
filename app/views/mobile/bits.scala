package views.html.mobile

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object bits {

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

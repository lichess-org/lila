package views.mobile

import lila.app.templating.Environment.{ *, given }

def apply(p: lila.cms.CmsPage.Render)(using PageContext) =
  views.base.layout(
    title = "Mobile",
    moreCss = cssTag("mobile"),
    withHrefLangs = lila.ui.LangPath(routes.Main.mobile).some
  ):
    main(
      div(cls := "mobile page-small box box-pad")(
        h1(cls := "box__top")(trans.site.playChessEverywhere()),
        div(cls := "sides")(
          div(cls := "left-side")(
            div(cls := "stores")(
              googlePlayButton,
              appleStoreButton
            ),
            div(cls := "apk")(views.cms.render(p)),
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
            img(
              cls     := "qrcode",
              widthA  := "200",
              heightA := "200",
              src     := assetUrl("images/mobile/qr-code.png"),
              alt     := "Download QR code"
            )
          )
        )
      )
    )

lazy val appleStoreButton = raw(
  """
<a class="store"
  href="https://itunes.apple.com/us/app/lichess-free-online-chess/id968371784">
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
  href="https://play.google.com/store/apps/details?id=org.lichess.mobileapp">
  <img alt="Android app on Google Play"
  width="192"
  height="74"
  src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" />
</a>
"""
)

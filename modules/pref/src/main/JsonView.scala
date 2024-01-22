package lila.pref

import play.api.libs.json._

object JsonView {

  implicit val customThemeWriter = Json.writes[CustomTheme]

  implicit val prefJsonWriter = OWrites[Pref] { p =>
    Json.obj(
      "dark"               -> p.dark,
      "transp"             -> p.transp,
      "bgImg"              -> p.bgImgOrDefault,
      "theme"              -> p.theme,
      "customTheme"        -> p.customThemeOrDefault,
      "pieceSet"           -> p.pieceSet,
      "chuPieceSet"        -> p.chuPieceSet,
      "kyoPieceSet"        -> p.kyoPieceSet,
      "soundSet"           -> p.soundSet,
      "notation"           -> p.notation,
      "blindfold"          -> p.blindfold,
      "takeback"           -> p.takeback,
      "moretime"           -> p.moretime,
      "clockTenths"        -> p.clockTenths,
      "clockCountdown"     -> p.clockCountdown,
      "clockSound"         -> p.clockSound,
      "premove"            -> p.premove,
      "boardLayout"        -> p.boardLayout,
      "animation"          -> p.animation,
      "clearHands"         -> p.clearHands,
      "handsBackground"    -> p.handsBackground,
      "follow"             -> p.follow,
      "highlightLastDests" -> p.highlightLastDests,
      "highlightCheck"     -> p.highlightCheck,
      "squareOverlay"      -> p.squareOverlay,
      "destination"        -> p.destination,
      "dropDestination"    -> p.dropDestination,
      "coords"             -> p.coords,
      "replay"             -> p.replay,
      "colorName"          -> p.colorName,
      "challenge"          -> p.challenge,
      "message"            -> p.message,
      "coordColor"         -> p.coordColor,
      "submitMove"         -> p.submitMove,
      "confirmResign"      -> p.confirmResign,
      "insightsShare"      -> p.insightsShare,
      "thickGrid"          -> p.thickGrid,
      "keyboardMove"       -> p.keyboardMove,
      "zen"                -> p.zen,
      "moveEvent"          -> p.moveEvent
    )
  }
}

package lidraughts.pref

import play.api.libs.json._

object JsonView {

  implicit val prefJsonWriter = OWrites[Pref] { p =>
    Json.obj(
      "dark" -> p.dark,
      "transp" -> p.transp,
      "bgImg" -> p.bgImgOrDefault,
      "theme" -> p.theme,
      "pieceSet" -> p.pieceSet,
      "soundSet" -> p.soundSet,
      "blindfold" -> p.blindfold,
      "autoThreefold" -> p.autoThreefold,
      "takeback" -> p.takeback,
      "moretime" -> p.moretime,
      "clockTenths" -> p.clockTenths,
      "clockBar" -> p.clockBar,
      "clockSound" -> p.clockSound,
      "premove" -> p.premove,
      "animation" -> p.animation,
      "captured" -> p.captured,
      "kingMoves" -> p.kingMoves,
      "follow" -> p.follow,
      "highlight" -> p.highlight,
      "destination" -> p.destination,
      "coords" -> p.coords,
      "replay" -> p.replay,
      "challenge" -> p.challenge,
      "message" -> p.message,
      "coordColor" -> p.coordColor,
      "submitMove" -> p.submitMove,
      "confirmResign" -> p.confirmResign,
      "insightShare" -> p.insightShare,
      "keyboardMove" -> p.keyboardMove,
      "fullCapture" -> p.fullCapture,
      "zen" -> p.zen,
      "moveEvent" -> p.moveEvent,
      "puzzleVariant" -> p.puzzleVariant.key
    )
  }
}

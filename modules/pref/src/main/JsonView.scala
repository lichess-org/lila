package lila.pref

import play.api.libs.json._

object JsonView {

  implicit val prefJsonWriter = OWrites[Pref] { p =>
    Json.obj(
      "dark" -> p.dark,
      "transp" -> p.transp,
      "bgImg" -> p.bgImg,
      "is3d" -> p.is3d,
      "theme" -> p.theme,
      "pieceSet" -> p.pieceSet,
      "theme3d" -> p.theme3d,
      "pieceSet3d" -> p.pieceSet3d,
      "soundSet" -> p.soundSet,
      "blindfold" -> p.blindfold,
      "autoQueen" -> p.autoQueen,
      "autoThreefold" -> p.autoThreefold,
      "takeback" -> p.takeback,
      "clockTenths" -> p.clockTenths,
      "clockBar" -> p.clockBar,
      "clockSound" -> p.clockSound,
      "premove" -> p.premove,
      "animation" -> p.animation,
      "captured" -> p.captured,
      "follow" -> p.follow,
      "highlight" -> p.highlight,
      "destination" -> p.destination,
      "coords" -> p.coords,
      "replay" -> p.replay,
      "challenge" -> p.challenge,
      "message" -> p.message,
      "coordColor" -> p.coordColor,
      "puzzleDifficulty" -> p.puzzleDifficulty,
      "submitMove" -> p.submitMove,
      "confirmResign" -> p.confirmResign,
      "insightShare" -> p.insightShare,
      "keyboardMove" -> p.keyboardMove,
      "moveEvent" -> p.moveEvent)
  }
}

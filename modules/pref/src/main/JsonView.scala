package lila.pref

import play.api.libs.json.*

import lila.common.Json.given

def toJson(p: Pref) = Json
  .obj(
    "bgImg" -> p.bgImgUrl,
    "is3d" -> p.is3d,
    "theme" -> p.theme,
    "pieceSet" -> p.pieceSet,
    "theme3d" -> p.theme3d,
    "pieceSet3d" -> p.pieceSet3d,
    "soundSet" -> p.soundSet,
    "autoQueen" -> p.autoQueen,
    "autoThreefold" -> p.autoThreefold,
    "takeback" -> p.takeback,
    "moretime" -> p.moretime,
    "clockTenths" -> p.clockTenths,
    "clockBar" -> p.clockBar,
    "clockSound" -> p.clockSound,
    "premove" -> p.premove,
    "animation" -> p.animation,
    "pieceNotation" -> p.pieceNotation,
    "captured" -> p.captured,
    "follow" -> p.follow,
    "highlight" -> p.highlight,
    "destination" -> p.destination,
    "coords" -> p.coords,
    "replay" -> p.replay,
    "challenge" -> p.challenge,
    "message" -> p.message,
    "submitMove" -> p.submitMove,
    "confirmResign" -> p.confirmResign,
    "insightShare" -> p.insightShare,
    "keyboardMove" -> p.keyboardMove,
    "voiceMove" -> p.hasVoice,
    "zen" -> p.zen,
    "ratings" -> p.ratings,
    "moveEvent" -> p.moveEvent,
    "rookCastle" -> p.rookCastle,
    "flairs" -> p.flairs,
    "sayGG" -> p.sayGG
  )
  .add("favFed", p.favFed)

def lichobile(p: Pref) = Json.obj(
  "animation" -> p.animation,
  "captured" -> p.captured,
  "highlight" -> p.highlight,
  "destination" -> p.destination,
  "coords" -> p.coords,
  "replay" -> p.replay,
  "premove" -> p.premove,
  "takeback" -> p.takeback,
  "autoQueen" -> p.autoQueen,
  "autoThreefold" -> p.autoThreefold,
  "confirmResign" -> p.confirmResign,
  "moretime" -> p.moretime,
  "clockTenths" -> p.clockTenths,
  "clockBar" -> p.clockBar,
  "clockSound" -> p.clockSound,
  "follow" -> p.follow,
  "challenge" -> p.challenge,
  "message" -> p.message,
  "submitMove" -> Pref.SubmitMove.lichobile.serverToApp(p.submitMove)
)

package lila.pref

import play.api.libs.json.*

object JsonView:

  def write(p: Pref, lichobileCompat: Boolean) =
    Json.obj(
      "dark"          -> (p.bg != Pref.Bg.LIGHT),
      "transp"        -> (p.bg == Pref.Bg.TRANSPARENT),
      "bgImg"         -> p.bgImgOrDefault,
      "is3d"          -> p.is3d,
      "theme"         -> p.theme,
      "pieceSet"      -> p.pieceSet,
      "theme3d"       -> p.theme3d,
      "pieceSet3d"    -> p.pieceSet3d,
      "soundSet"      -> p.soundSet,
      "autoQueen"     -> p.autoQueen,
      "autoThreefold" -> p.autoThreefold,
      "takeback"      -> p.takeback,
      "moretime"      -> p.moretime,
      "clockTenths"   -> p.clockTenths,
      "clockBar"      -> p.clockBar,
      "clockSound"    -> p.clockSound,
      "premove"       -> p.premove,
      "animation"     -> p.animation,
      "pieceNotation" -> p.pieceNotation,
      "captured"      -> p.captured,
      "follow"        -> p.follow,
      "highlight"     -> p.highlight,
      "destination"   -> p.destination,
      "coords"        -> p.coords,
      "replay"        -> p.replay,
      "challenge"     -> p.challenge,
      "message"       -> p.message,
      "submitMove" -> {
        if lichobileCompat then Pref.SubmitMove.lichobile.serverToApp(p.submitMove)
        else p.submitMove
      },
      "confirmResign" -> p.confirmResign,
      "insightShare"  -> p.insightShare,
      "keyboardMove"  -> p.keyboardMove,
      "voiceMove"     -> p.hasVoice,
      "zen"           -> p.zen,
      "ratings"       -> p.ratings,
      "moveEvent"     -> p.moveEvent,
      "rookCastle"    -> p.rookCastle
    )

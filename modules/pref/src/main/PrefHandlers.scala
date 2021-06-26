package lila.pref

import reactivemongo.api.bson._

import lila.db.BSON
import lila.db.dsl._

private object PrefHandlers {

  implicit val prefBSONHandler = new BSON[Pref] {

    def reads(r: BSON.Reader): Pref =
      Pref(
        _id = r str "_id",
        bg = r.getD("bg", Pref.default.bg),
        bgImg = r.strO("bgImg"),
        is3d = r.getD("is3d", Pref.default.is3d),
        theme = r.getD("theme", Pref.default.theme),
        pieceSet = r.getD("pieceSet", Pref.default.pieceSet),
        theme3d = r.getD("theme3d", Pref.default.theme3d),
        pieceSet3d = r.getD("pieceSet3d", Pref.default.pieceSet3d),
        soundSet = r.getD("soundSet", Pref.default.soundSet),
        blindfold = r.getD("blindfold", Pref.default.blindfold),
        autoQueen = r.getD("autoQueen", Pref.default.autoQueen),
        autoThreefold = r.getD("autoThreefold", Pref.default.autoThreefold),
        takeback = r.getD("takeback", Pref.default.takeback),
        moretime = r.getD("moretime", Pref.default.moretime),
        clockTenths = r.getD("clockTenths", Pref.default.clockTenths),
        clockBar = r.getD("clockBar", Pref.default.clockBar),
        clockSound = r.getD("clockSound", Pref.default.clockSound),
        premove = r.getD("premove", Pref.default.premove),
        animation = r.getD("animation", Pref.default.animation),
        captured = r.getD("captured", Pref.default.captured),
        follow = r.getD("follow", Pref.default.follow),
        highlight = r.getD("highlight", Pref.default.highlight),
        destination = r.getD("destination", Pref.default.destination),
        coords = r.getD("coords", Pref.default.coords),
        replay = r.getD("replay", Pref.default.replay),
        challenge = r.getD("challenge", Pref.default.challenge),
        message = r.getD("message", Pref.default.message),
        studyInvite = r.getD("studyInvite", Pref.default.studyInvite),
        coordColor = r.getD("coordColor", Pref.default.coordColor),
        submitMove = r.getD("submitMove", Pref.default.submitMove),
        confirmResign = r.getD("confirmResign", Pref.default.confirmResign),
        mention = r.getD("mention", Pref.default.mention),
        insightShare = r.getD("insightShare", Pref.default.insightShare),
        keyboardMove = r.getD("keyboardMove", Pref.default.keyboardMove),
        zen = r.getD("zen", Pref.default.zen),
        rookCastle = r.getD("rookCastle", Pref.default.rookCastle),
        scrollMoves = r.getD("scrollMoves", Pref.default.scrollMoves),
        pieceNotation = r.getD("pieceNotation", Pref.default.pieceNotation),
        resizeHandle = r.getD("resizeHandle", Pref.default.resizeHandle),
        moveEvent = r.getD("moveEvent", Pref.default.moveEvent),
        tags = r.getD("tags", Pref.default.tags)
      )

    def writes(w: BSON.Writer, o: Pref) =
      $doc(
        "_id"           -> o._id,
        "bg"            -> o.bg,
        "bgImg"         -> o.bgImg,
        "is3d"          -> o.is3d,
        "theme"         -> o.theme,
        "pieceSet"      -> o.pieceSet,
        "theme3d"       -> o.theme3d,
        "pieceSet3d"    -> o.pieceSet3d,
        "soundSet"      -> SoundSet.name2key(o.soundSet),
        "blindfold"     -> o.blindfold,
        "autoQueen"     -> o.autoQueen,
        "autoThreefold" -> o.autoThreefold,
        "takeback"      -> o.takeback,
        "moretime"      -> o.moretime,
        "clockTenths"   -> o.clockTenths,
        "clockBar"      -> o.clockBar,
        "clockSound"    -> o.clockSound,
        "premove"       -> o.premove,
        "animation"     -> o.animation,
        "captured"      -> o.captured,
        "follow"        -> o.follow,
        "highlight"     -> o.highlight,
        "destination"   -> o.destination,
        "coords"        -> o.coords,
        "replay"        -> o.replay,
        "challenge"     -> o.challenge,
        "message"       -> o.message,
        "studyInvite"   -> o.studyInvite,
        "coordColor"    -> o.coordColor,
        "submitMove"    -> o.submitMove,
        "confirmResign" -> o.confirmResign,
        "mention"       -> o.mention,
        "insightShare"  -> o.insightShare,
        "keyboardMove"  -> o.keyboardMove,
        "zen"           -> o.zen,
        "rookCastle"    -> o.rookCastle,
        "scrollMoves"   -> o.scrollMoves,
        "moveEvent"     -> o.moveEvent,
        "pieceNotation" -> o.pieceNotation,
        "resizeHandle"  -> o.resizeHandle,
        "tags"          -> o.tags
      )
  }
}

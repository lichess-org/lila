package lila.pref

import reactivemongo.api.bson._

import lila.db.BSON
import lila.db.dsl._

private object PrefHandlers {

  implicit val prefBSONHandler = new BSON[Pref] {
    
    def reads(r: BSON.Reader): Pref =
      Pref(
        _id = r str "_id",
        dark = r.getD("dark", Pref.default.dark),
        transp = r.getD("transp", Pref.default.transp),
        bgImg = r.strO("bgImg"),
        theme = r.getD("theme", Pref.default.theme),
        pieceSet = r.getD("pieceSet", Pref.default.pieceSet),
        soundSet = r.getD("soundSet", Pref.default.soundSet),
        blindfold = r.getD("blindfold", Pref.default.blindfold),
        takeback = r.getD("takeback", Pref.default.takeback),
        moretime = r.getD("moretime", Pref.default.moretime),
        clockTenths = r.getD("clockTenths", Pref.default.clockTenths),
        clockCountdown = r.getD("clockCountdown", Pref.default.clockCountdown),
        clockBar = r.getD("clockBar", Pref.default.clockBar),
        clockSound = r.getD("clockSound", Pref.default.clockSound),
        premove = r.getD("premove", Pref.default.premove),
        animation = r.getD("animation", Pref.default.animation),
        captured = r.getD("captured", Pref.default.captured),
        follow = r.getD("follow", Pref.default.follow),
        highlightLastDests = r.getD("highlightLastDests", Pref.default.highlightLastDests),
        highlightCheck = r.getD("highlightCheck", Pref.default.highlightCheck),
        destination = r.getD("destination", Pref.default.destination),
        dropDestination = r.getD("dropDestination", Pref.default.dropDestination),
        coords = r.getD("coords", Pref.default.coords),
        replay = r.getD("replay", Pref.default.replay),
        challenge = r.getD("challenge", Pref.default.challenge),
        message = r.getD("message", Pref.default.message),
        studyInvite = r.getD("studyInvite", Pref.default.studyInvite),
        coordColor = r.getD("coordColor", Pref.default.coordColor),
        submitMove = r.getD("submitMove", Pref.default.submitMove),
        confirmResign = r.getD("confirmResign", Pref.default.confirmResign),
        insightShare = r.getD("insightShare", Pref.default.insightShare),
        keyboardMove = r.getD("keyboardMove", Pref.default.keyboardMove),
        zen = r.getD("zen", Pref.default.zen),
        notation = r.getD("notation", Pref.default.notation),
        resizeHandle = r.getD("resizeHandle", Pref.default.resizeHandle),
        moveEvent = r.getD("moveEvent", Pref.default.moveEvent),
        tags = r.getD("tags", Pref.default.tags)
      )

    def writes(w: BSON.Writer, o: Pref) =
      $doc(
        "_id"             -> o._id,
        "dark"            -> o.dark,
        "transp"          -> o.transp,
        "bgImg"           -> o.bgImg,
        "theme"           -> o.theme,
        "pieceSet"        -> o.pieceSet,
        "soundSet"        -> SoundSet.name2key(o.soundSet),
        "blindfold"       -> o.blindfold,
        "takeback"        -> o.takeback,
        "moretime"        -> o.moretime,
        "clockTenths"     -> o.clockTenths,
        "clockCountdown"  -> o.clockCountdown,
        "clockBar"        -> o.clockBar,
        "clockSound"      -> o.clockSound,
        "premove"         -> o.premove,
        "animation"       -> o.animation,
        "captured"        -> o.captured,
        "follow"          -> o.follow,
        "highlightLastDests" -> o.highlightLastDests,
        "highlightCheck"    -> o.highlightCheck,
        "destination"     -> o.destination,
        "dropDestination" -> o.dropDestination,
        "coords"          -> o.coords,
        "replay"          -> o.replay,
        "challenge"       -> o.challenge,
        "message"         -> o.message,
        "studyInvite"     -> o.studyInvite,
        "coordColor"      -> o.coordColor,
        "submitMove"      -> o.submitMove,
        "confirmResign"   -> o.confirmResign,
        "insightShare"    -> o.insightShare,
        "keyboardMove"    -> o.keyboardMove,
        "zen"             -> o.zen,
        "moveEvent"       -> o.moveEvent,
        "notation"        -> o.notation,
        "resizeHandle"    -> o.resizeHandle,
        "tags"            -> o.tags
      )
  }
}

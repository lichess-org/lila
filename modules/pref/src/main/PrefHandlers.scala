package lila.pref

import reactivemongo.api.bson.*

import lila.db.BSON
import lila.db.dsl.{ *, given }

private object PrefHandlers:

  given BSONDocumentHandler[Pref.BoardPref] = Macros.handler

  given BSONDocumentHandler[Pref] = new BSON[Pref]:

    def reads(r: BSON.Reader): Pref =
      Pref(
        id = r.get[UserId]("_id"),
        bg = r.getD("bg", Pref.default.bg),
        bgImg = r.strO("bgImg"),
        is3d = r.getD("is3d", Pref.default.is3d),
        theme = r.getD("theme", Pref.default.theme),
        pieceSet = r.getD("pieceSet", Pref.default.pieceSet),
        theme3d = r.getD("theme3d", Pref.default.theme3d),
        pieceSet3d = r.getD("pieceSet3d", Pref.default.pieceSet3d),
        soundSet = r.getD("soundSet", Pref.default.soundSet),
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
        submitMove = r.getD("submitMove", Pref.default.submitMove),
        confirmResign = r.getD("confirmResign", Pref.default.confirmResign),
        insightShare = r.getD("insightShare", Pref.default.insightShare),
        keyboardMove = r.getD("keyboardMove", Pref.default.keyboardMove),
        voice = r.getO("voice"),
        zen = r.getD("zen", Pref.default.zen),
        ratings = r.getD("ratings", Pref.default.ratings),
        flairs = r.getD("flairs", Pref.default.flairs),
        rookCastle = r.getD("rookCastle", Pref.default.rookCastle),
        pieceNotation = r.getD("pieceNotation", Pref.default.pieceNotation),
        resizeHandle = r.getD("resizeHandle", Pref.default.resizeHandle),
        moveEvent = r.getD("moveEvent", Pref.default.moveEvent),
        agreement = r.getD("agreement", 0),
        board = r.getD("board", Pref.default.board),
        usingAltSocket = r.getO("usingAltSocket"),
        tags = r.getD("tags", Pref.default.tags)
      )

    def writes(w: BSON.Writer, o: Pref) =
      $doc(
        "_id"            -> o.id,
        "bg"             -> o.bg,
        "bgImg"          -> o.bgImg,
        "is3d"           -> o.is3d,
        "theme"          -> o.theme,
        "pieceSet"       -> o.pieceSet,
        "theme3d"        -> o.theme3d,
        "pieceSet3d"     -> o.pieceSet3d,
        "soundSet"       -> SoundSet.name2key(o.soundSet),
        "autoQueen"      -> o.autoQueen,
        "autoThreefold"  -> o.autoThreefold,
        "takeback"       -> o.takeback,
        "moretime"       -> o.moretime,
        "clockTenths"    -> o.clockTenths,
        "clockBar"       -> o.clockBar,
        "clockSound"     -> o.clockSound,
        "premove"        -> o.premove,
        "animation"      -> o.animation,
        "captured"       -> o.captured,
        "follow"         -> o.follow,
        "highlight"      -> o.highlight,
        "destination"    -> o.destination,
        "coords"         -> o.coords,
        "replay"         -> o.replay,
        "challenge"      -> o.challenge,
        "message"        -> o.message,
        "studyInvite"    -> o.studyInvite,
        "submitMove"     -> o.submitMove,
        "confirmResign"  -> o.confirmResign,
        "insightShare"   -> o.insightShare,
        "keyboardMove"   -> o.keyboardMove,
        "voice"          -> o.voice,
        "zen"            -> o.zen,
        "ratings"        -> o.ratings,
        "flairs"         -> o.flairs,
        "rookCastle"     -> o.rookCastle,
        "moveEvent"      -> o.moveEvent,
        "pieceNotation"  -> o.pieceNotation,
        "resizeHandle"   -> o.resizeHandle,
        "agreement"      -> o.agreement,
        "usingAltSocket" -> o.usingAltSocket,
        "board"          -> o.board,
        "tags"           -> o.tags
      )

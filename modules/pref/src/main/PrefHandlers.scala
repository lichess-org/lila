package lila.pref

import reactivemongo.api.bson.*

import lila.core.ublog.QualityFilter as BlogQualityFilter
import lila.db.BSON
import lila.db.dsl.{ *, given }

private object PrefHandlers:

  given BSONDocumentHandler[Pref.BoardPref] = new BSON[Pref.BoardPref]:

    def reads(r: BSON.Reader): Pref.BoardPref =
      val d = Pref.default.board
      Pref.BoardPref(
        brightness = r.getD("brightness", d.brightness),
        contrast = r.getD("contrast", d.contrast),
        opacity = r.getD("opacity", d.opacity),
        hue = r.getD("hue", d.hue)
      )

    def writes(w: BSON.Writer, o: Pref.BoardPref) =
      $doc(
        "brightness" -> o.brightness,
        "contrast" -> o.contrast,
        "opacity" -> o.opacity,
        "hue" -> o.hue
      )

  given BSONDocumentHandler[Pref] = new BSON[Pref]:

    def reads(r: BSON.Reader): Pref =
      val d = Pref.default
      Pref(
        id = r.get[UserId]("_id"),
        bg = r.getD("bg", d.bg),
        bgImg = r.strO("bgImg"),
        is3d = r.getD("is3d", d.is3d),
        theme = r.getD("theme", d.theme),
        pieceSet = r.getD("pieceSet", d.pieceSet),
        theme3d = r.getD("theme3d", d.theme3d),
        pieceSet3d = r.getD("pieceSet3d", d.pieceSet3d),
        soundSet = r.getD("soundSet", d.soundSet),
        autoQueen = r.getD("autoQueen", d.autoQueen),
        autoThreefold = r.getD("autoThreefold", d.autoThreefold),
        takeback = r.getD("takeback", d.takeback),
        moretime = r.getD("moretime", d.moretime),
        clockTenths = r.getD("clockTenths", d.clockTenths),
        clockBar = r.getD("clockBar", d.clockBar),
        clockSound = r.getD("clockSound", d.clockSound),
        premove = r.getD("premove", d.premove),
        animation = r.getD("animation", d.animation),
        captured = r.getD("captured", d.captured),
        follow = r.getD("follow", d.follow),
        highlight = r.getD("highlight", d.highlight),
        destination = r.getD("destination", d.destination),
        coords = r.getD("coords", d.coords),
        replay = r.getD("replay", d.replay),
        challenge = r.getD("challenge", d.challenge),
        message = r.getD("message", d.message),
        studyInvite = r.getD("studyInvite", d.studyInvite),
        submitMove = r.getD("submitMove", d.submitMove),
        confirmResign = r.getD("confirmResign", d.confirmResign),
        insightShare = r.getD("insightShare", d.insightShare),
        keyboardMove = r.getD("keyboardMove", d.keyboardMove),
        voice = r.getO("voice"),
        zen = r.getD("zen", d.zen),
        ratings = r.getD("ratings", d.ratings),
        flairs = r.getD("flairs", d.flairs),
        rookCastle = r.getD("rookCastle", d.rookCastle),
        pieceNotation = r.getD("pieceNotation", d.pieceNotation),
        resizeHandle = r.getD("resizeHandle", d.resizeHandle),
        moveEvent = r.getD("moveEvent", d.moveEvent),
        agreement = r.getD("agreement", 0),
        board = r.getD("board", d.board),
        blogFilter = r.strO("blogFilter").flatMap(BlogQualityFilter.fromName) | d.blogFilter,
        usingAltSocket = r.getO("usingAltSocket"),
        sayGG = r.getD("sayGG", d.sayGG),
        tags = r.getD("tags", d.tags)
      )

    def writes(w: BSON.Writer, o: Pref) =
      $doc(
        "_id" -> o.id,
        "bg" -> o.bg,
        "bgImg" -> o.bgImg,
        "is3d" -> o.is3d,
        "theme" -> o.theme,
        "pieceSet" -> o.pieceSet,
        "theme3d" -> o.theme3d,
        "pieceSet3d" -> o.pieceSet3d,
        "soundSet" -> SoundSet.name2key(o.soundSet),
        "autoQueen" -> o.autoQueen,
        "autoThreefold" -> o.autoThreefold,
        "takeback" -> o.takeback,
        "moretime" -> o.moretime,
        "clockTenths" -> o.clockTenths,
        "clockBar" -> o.clockBar,
        "clockSound" -> o.clockSound,
        "premove" -> o.premove,
        "animation" -> o.animation,
        "captured" -> o.captured,
        "follow" -> o.follow,
        "highlight" -> o.highlight,
        "destination" -> o.destination,
        "coords" -> o.coords,
        "replay" -> o.replay,
        "challenge" -> o.challenge,
        "message" -> o.message,
        "studyInvite" -> o.studyInvite,
        "submitMove" -> o.submitMove,
        "confirmResign" -> o.confirmResign,
        "insightShare" -> o.insightShare,
        "keyboardMove" -> o.keyboardMove,
        "voice" -> o.voice,
        "zen" -> o.zen,
        "ratings" -> o.ratings,
        "flairs" -> o.flairs,
        "rookCastle" -> o.rookCastle,
        "moveEvent" -> o.moveEvent,
        "pieceNotation" -> o.pieceNotation,
        "resizeHandle" -> o.resizeHandle,
        "agreement" -> o.agreement,
        "usingAltSocket" -> o.usingAltSocket,
        "board" -> o.board,
        "blogFilter" -> o.blogFilter.ordinal,
        "sayGG" -> o.sayGG,
        "tags" -> o.tags
      )

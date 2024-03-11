package lila.pref

import reactivemongo.api.bson._

import lila.db.BSON
import lila.db.dsl._

private object PrefHandlers {

  implicit val customThemeBSONHandler = new BSON[CustomTheme] {

    def reads(r: BSON.Reader): CustomTheme =
      CustomTheme(
        boardColor = r str "bc",
        boardImg = r str "bi",
        gridColor = r str "gc",
        gridWidth = r int "gw",
        handsColor = r str "hc",
        handsImg = r str "hi"
      )

    def writes(w: BSON.Writer, o: CustomTheme) =
      BSONDocument(
        "bc" -> w.str(o.boardColor),
        "bi" -> w.str(o.boardImg),
        "gc" -> w.str(o.gridColor),
        "gw" -> w.int(o.gridWidth),
        "hc" -> w.str(o.handsColor),
        "hi" -> w.str(o.handsImg)
      )
  }

  implicit val prefBSONHandler = new BSON[Pref] {

    def reads(r: BSON.Reader): Pref =
      Pref(
        _id = r str "_id",
        dark = r.getD("dark", Pref.default.dark),
        transp = r.getD("transp", Pref.default.transp),
        bgImg = r.strO("bgImg"),
        theme = r.getD("theme", Pref.default.theme),
        customTheme = r.getO[CustomTheme]("customTheme"),
        pieceSet = r.getD("pieceSet", Pref.default.pieceSet),
        chuPieceSet = r.getD("chuPieceSet", Pref.default.chuPieceSet),
        kyoPieceSet = r.getD("kyoPieceSet", Pref.default.kyoPieceSet),
        soundSet = r.getD("soundSet", Pref.default.soundSet),
        blindfold = r.getD("blindfold", Pref.default.blindfold),
        takeback = r.getD("takeback", Pref.default.takeback),
        moretime = r.getD("moretime", Pref.default.moretime),
        clockTenths = r.getD("clockTenths", Pref.default.clockTenths),
        clockCountdown = r.getD("clockCountdown", Pref.default.clockCountdown),
        clockSound = r.getD("clockSound", Pref.default.clockSound),
        premove = r.getD("premove", Pref.default.premove),
        boardLayout = r.getD("boardLayout", Pref.default.boardLayout),
        animation = r.getD("animation", Pref.default.animation),
        coords = r.getD("coords", Pref.default.coords),
        clearHands = r.getD("clearHands", Pref.default.clearHands),
        handsBackground = r.getD("handsBackground", Pref.default.handsBackground),
        follow = r.getD("follow", Pref.default.follow),
        highlightLastDests = r.getD("highlightLastDests", Pref.default.highlightLastDests),
        highlightCheck = r.getD("highlightCheck", Pref.default.highlightCheck),
        destination = r.getD("destination", Pref.default.destination),
        dropDestination = r.getD("dropDestination", Pref.default.dropDestination),
        replay = r.getD("replay", Pref.default.replay),
        colorName = r.getD("colorName", Pref.default.colorName),
        challenge = r.getD("challenge", Pref.default.challenge),
        message = r.getD("message", Pref.default.message),
        studyInvite = r.getD("studyInvite", Pref.default.studyInvite),
        coordColor = r.getD("coordColor", Pref.default.coordColor),
        submitMove = r.getD("submitMove", Pref.default.submitMove),
        confirmResign = r.getD("confirmResign", Pref.default.confirmResign),
        insightsShare = r.getD("insightsShare", Pref.default.insightsShare),
        thickGrid = r.getD("thickGrid", Pref.default.thickGrid),
        keyboardMove = r.getD("keyboardMove", Pref.default.keyboardMove),
        zen = r.getD("zen", Pref.default.zen),
        notation = r.getD("notation", Pref.default.notation),
        resizeHandle = r.getD("resizeHandle", Pref.default.resizeHandle),
        squareOverlay = r.getD("squareOverlay", Pref.default.squareOverlay),
        moveEvent = r.getD("moveEvent", Pref.default.moveEvent),
        tags = r.getD("tags", Pref.default.tags)
      )

    def writes(w: BSON.Writer, o: Pref) =
      $doc(
        "_id"                -> o._id,
        "dark"               -> o.dark,
        "transp"             -> o.transp,
        "bgImg"              -> o.bgImg,
        "theme"              -> o.theme,
        "customTheme"        -> o.customTheme,
        "pieceSet"           -> o.pieceSet,
        "chuPieceSet"        -> o.chuPieceSet,
        "kyoPieceSet"        -> o.kyoPieceSet,
        "soundSet"           -> o.soundSet,
        "blindfold"          -> o.blindfold,
        "takeback"           -> o.takeback,
        "moretime"           -> o.moretime,
        "clockTenths"        -> o.clockTenths,
        "clockCountdown"     -> o.clockCountdown,
        "clockSound"         -> o.clockSound,
        "premove"            -> o.premove,
        "boardLayout"        -> o.boardLayout,
        "animation"          -> o.animation,
        "coords"             -> o.coords,
        "clearHands"         -> o.clearHands,
        "handsBackground"    -> o.handsBackground,
        "follow"             -> o.follow,
        "highlightLastDests" -> o.highlightLastDests,
        "highlightCheck"     -> o.highlightCheck,
        "squareOverlay"      -> o.squareOverlay,
        "destination"        -> o.destination,
        "dropDestination"    -> o.dropDestination,
        "replay"             -> o.replay,
        "colorName"          -> o.colorName,
        "challenge"          -> o.challenge,
        "message"            -> o.message,
        "studyInvite"        -> o.studyInvite,
        "coordColor"         -> o.coordColor,
        "submitMove"         -> o.submitMove,
        "confirmResign"      -> o.confirmResign,
        "insightsShare"      -> o.insightsShare,
        "thickGrid"          -> o.thickGrid,
        "keyboardMove"       -> o.keyboardMove,
        "zen"                -> o.zen,
        "moveEvent"          -> o.moveEvent,
        "notation"           -> o.notation,
        "resizeHandle"       -> o.resizeHandle,
        "tags"               -> o.tags
      )
  }
}

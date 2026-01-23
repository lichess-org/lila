package lila.pref

import play.api.data.*
import play.api.data.Forms.*

import lila.common.Form.{ numberIn, stringIn, tolerantBoolean }

object PrefForm:

  private def containedIn(choices: Seq[(Int, String)]): Int => Boolean =
    choice => choices.exists(_._1 == choice)

  private def checkedNumber(choices: Seq[(Int, String)]) =
    number.verifying(containedIn(choices))

  private def bitPresent(anInt: Int, bit: Int): Boolean =
    (anInt & bit) == bit

  private def bitContainedIn(choices: Seq[(Int, String)]): Int => Boolean =
    choice => choice == 0 || choices.exists((bit, _) => bitPresent(choice, bit))

  private def bitCheckedNumber(choices: Seq[(Int, String)]) =
    number.verifying(bitContainedIn(choices))

  private lazy val booleanNumber =
    number.verifying(Pref.BooleanPref.verify)

  object fields:
    val theme = "theme" -> text.verifying(Theme.contains(_))
    val theme3d = "theme3d" -> text.verifying(Theme3d.contains(_))
    val pieceSet = "pieceSet" -> text.verifying(PieceSet.contains(_))
    val pieceSet3d = "pieceSet3d" -> text.verifying(PieceSet3d.contains(_))
    val soundSet = "soundSet" -> text.verifying(SoundSet.contains(_))
    val bg = "bg" -> stringIn(Pref.Bg.fromString.keySet)
    val bgImg = "bgImg" -> text(maxLength = 400).verifying(
      "URL must use https",
      url => url.isBlank || url.startsWith("https://") || url.startsWith("//")
    )
    val is3d = "is3d" -> tolerantBoolean
    val zen = "zen" -> checkedNumber(Pref.Zen.choices)
    val voice = "voice" -> booleanNumber
    val keyboardMove = "keyboardMove" -> booleanNumber
    val autoQueen = "autoQueen" -> checkedNumber(Pref.AutoQueen.choices)
    val premove = "premove" -> booleanNumber
    val takeback = "takeback" -> checkedNumber(Pref.Takeback.choices)
    val autoThreefold = "autoThreefold" -> checkedNumber(Pref.AutoThreefold.choices)
    val submitMove = "submitMove" -> bitCheckedNumber(Pref.SubmitMove.choices)
    val confirmResign = "confirmResign" -> checkedNumber(Pref.ConfirmResign.choices)
    val moretime = "moretime" -> checkedNumber(Pref.Moretime.choices)
    val clockSound = "clockSound" -> booleanNumber
    val pieceNotation = "pieceNotation" -> booleanNumber
    val ratings = "ratings" -> checkedNumber(Pref.Ratings.choices)
    val flairs = "flairs" -> boolean
    val follow = "follow" -> booleanNumber
    val challenge = "challenge" -> checkedNumber(Pref.Challenge.choices)
    val message = "message" -> checkedNumber(Pref.Message.choices)
    object board:
      val brightness = "boardBrightness" -> number(20, 140)
      val contrast = "boardContrast" -> number(40, 200)
      val opacity = "boardOpacity" -> number(0, 100)
      val hue = "boardHue" -> number(0, 100)
    val sayGG = "sayGG" -> checkedNumber(Pref.SayGG.choices)

  def pref(lichobile: Boolean) = Form(
    mapping(
      "display" -> mapping(
        "animation" -> numberIn(Set(0, 1, 2, 3)),
        "captured" -> booleanNumber,
        "highlight" -> booleanNumber,
        "destination" -> booleanNumber,
        "coords" -> checkedNumber(Pref.Coords.choices),
        "replay" -> checkedNumber(Pref.Replay.choices),
        "pieceNotation" -> optional(booleanNumber),
        fields.zen.map2(optional),
        "resizeHandle" -> optional(checkedNumber(Pref.ResizeHandle.choices))
      )(DisplayData.apply)(unapply),
      "behavior" -> mapping(
        "moveEvent" -> optional(numberIn(Set(0, 1, 2))),
        fields.premove,
        fields.takeback,
        fields.autoQueen,
        fields.autoThreefold,
        fields.submitMove.map2: mapping =>
          if lichobile then
            import Pref.SubmitMove.lichobile as compat
            optional(numberIn(compat.choices).transform(compat.appToServer, compat.serverToApp))
          else optional(mapping),
        fields.confirmResign,
        fields.keyboardMove.map2(optional),
        fields.voice.map2(optional),
        "rookCastle" -> optional(booleanNumber),
        fields.sayGG.map2(optional)
      )(BehaviorData.apply)(unapply),
      "clock" -> mapping(
        "tenths" -> checkedNumber(Pref.ClockTenths.choices),
        "bar" -> booleanNumber,
        "sound" -> booleanNumber,
        fields.moretime
      )(ClockData.apply)(unapply),
      fields.follow,
      fields.challenge,
      fields.message,
      "studyInvite" -> optional(checkedNumber(Pref.StudyInvite.choices)),
      "insightShare" -> numberIn(Set(0, 1, 2)),
      fields.ratings.map2(optional),
      fields.flairs.map2(optional)
    )(PrefData.apply)(unapply)
  )

  case class DisplayData(
      animation: Int,
      captured: Int,
      highlight: Int,
      destination: Int,
      coords: Int,
      replay: Int,
      pieceNotation: Option[Int],
      zen: Option[Int],
      resizeHandle: Option[Int]
  )

  case class BehaviorData(
      moveEvent: Option[Int],
      premove: Int,
      takeback: Int,
      autoQueen: Int,
      autoThreefold: Int,
      submitMove: Option[Int],
      confirmResign: Int,
      keyboardMove: Option[Int],
      voice: Option[Int],
      rookCastle: Option[Int],
      sayGG: Option[Int]
  )

  case class ClockData(
      tenths: Int,
      bar: Int,
      sound: Int,
      moretime: Int
  )

  case class PrefData(
      display: DisplayData,
      behavior: BehaviorData,
      clock: ClockData,
      follow: Int,
      challenge: Int,
      message: Int,
      studyInvite: Option[Int],
      insightShare: Int,
      ratings: Option[Int],
      flairs: Option[Boolean]
  ):

    def apply(pref: Pref) =
      pref.copy(
        autoQueen = behavior.autoQueen,
        autoThreefold = behavior.autoThreefold,
        takeback = behavior.takeback,
        moretime = clock.moretime,
        clockTenths = clock.tenths,
        clockBar = clock.bar == 1,
        clockSound = clock.sound == 1,
        follow = follow == 1,
        highlight = display.highlight == 1,
        destination = display.destination == 1,
        coords = display.coords,
        replay = display.replay,
        challenge = challenge,
        message = message,
        studyInvite = studyInvite | Pref.default.studyInvite,
        premove = behavior.premove == 1,
        animation = display.animation,
        submitMove = behavior.submitMove.getOrElse(0),
        insightShare = insightShare,
        confirmResign = behavior.confirmResign,
        captured = display.captured == 1,
        keyboardMove = behavior.keyboardMove | pref.keyboardMove,
        voice = if pref.voice.isEmpty && !behavior.voice.contains(1) then None else behavior.voice,
        zen = display.zen | pref.zen,
        ratings = ratings | pref.ratings,
        flairs = flairs | pref.flairs,
        resizeHandle = display.resizeHandle | pref.resizeHandle,
        rookCastle = behavior.rookCastle | pref.rookCastle,
        sayGG = behavior.sayGG | pref.sayGG,
        pieceNotation = display.pieceNotation | pref.pieceNotation,
        moveEvent = behavior.moveEvent | pref.moveEvent
      )

  object PrefData:
    def apply(pref: Pref): PrefData =
      PrefData(
        display = DisplayData(
          highlight = if pref.highlight then 1 else 0,
          destination = if pref.destination then 1 else 0,
          animation = pref.animation,
          coords = pref.coords,
          replay = pref.replay,
          captured = if pref.captured then 1 else 0,
          zen = pref.zen.some,
          resizeHandle = pref.resizeHandle.some,
          pieceNotation = pref.pieceNotation.some
        ),
        behavior = BehaviorData(
          moveEvent = pref.moveEvent.some,
          premove = if pref.premove then 1 else 0,
          takeback = pref.takeback,
          autoQueen = pref.autoQueen,
          autoThreefold = pref.autoThreefold,
          submitMove = pref.submitMove.some,
          confirmResign = pref.confirmResign,
          keyboardMove = pref.keyboardMove.some,
          voice = pref.voice.getOrElse(0).some,
          rookCastle = pref.rookCastle.some,
          sayGG = pref.sayGG.some
        ),
        clock = ClockData(
          tenths = pref.clockTenths,
          bar = if pref.clockBar then 1 else 0,
          sound = if pref.clockSound then 1 else 0,
          moretime = pref.moretime
        ),
        follow = if pref.follow then 1 else 0,
        challenge = pref.challenge,
        message = pref.message,
        studyInvite = pref.studyInvite.some,
        insightShare = pref.insightShare,
        ratings = pref.ratings.some,
        flairs = pref.flairs.some
      )

  def prefOf(p: Pref): Form[PrefData] = pref(lichobile = false).fill(PrefData(p))

  val cfRoutingForm = Form(single("cfRouting" -> boolean))

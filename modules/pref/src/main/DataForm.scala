package lidraughts.pref

import play.api.data._
import play.api.data.Forms._

object DataForm {

  private def containedIn(choices: Seq[(Int, String)]): Int => Boolean =
    choice => choices.exists(_._1 == choice)

  private def checkedNumber(choices: Seq[(Int, String)]) =
    number.verifying(containedIn(choices))

  private lazy val booleanNumber =
    number.verifying(Pref.BooleanPref.verify)

  val pref = Form(mapping(
    "display" -> mapping(
      "animation" -> number.verifying(Set(0, 1, 2, 3) contains _),
      "captured" -> booleanNumber,
      "kingMoves" -> booleanNumber,
      "highlight" -> booleanNumber,
      "destination" -> booleanNumber,
      "coords" -> checkedNumber(Pref.Coords.choices),
      "replay" -> checkedNumber(Pref.Replay.choices),
      "gameResult" -> checkedNumber(Pref.GameResult.choices),
      "coordSystem" -> checkedNumber(Pref.CoordSystem.choices),
      "zen" -> optional(booleanNumber),
      "resizeHandle" -> optional(checkedNumber(Pref.ResizeHandle.choices)),
      "blindfold" -> checkedNumber(Pref.Blindfold.choices)
    )(DisplayData.apply)(DisplayData.unapply),
    "behavior" -> mapping(
      "moveEvent" -> optional(number.verifying(Set(0, 1, 2) contains _)),
      "premove" -> booleanNumber,
      "takeback" -> checkedNumber(Pref.Takeback.choices),
      "autoThreefold" -> checkedNumber(Pref.AutoThreefold.choices),
      "submitMove" -> checkedNumber(Pref.SubmitMove.choices),
      "confirmResign" -> checkedNumber(Pref.ConfirmResign.choices),
      "keyboardMove" -> optional(booleanNumber),
      "fullCapture" -> optional(booleanNumber)
    )(BehaviorData.apply)(BehaviorData.unapply),
    "clock" -> mapping(
      "tenths" -> checkedNumber(Pref.ClockTenths.choices),
      "bar" -> booleanNumber,
      "sound" -> booleanNumber,
      "moretime" -> checkedNumber(Pref.Moretime.choices)
    )(ClockData.apply)(ClockData.unapply),
    "follow" -> booleanNumber,
    "challenge" -> checkedNumber(Pref.Challenge.choices),
    "message" -> checkedNumber(Pref.Message.choices),
    "studyInvite" -> optional(checkedNumber(Pref.StudyInvite.choices)),
    "insightShare" -> number.verifying(Set(0, 1, 2) contains _)
  )(PrefData.apply)(PrefData.unapply))

  case class DisplayData(
      animation: Int,
      captured: Int,
      kingMoves: Int,
      highlight: Int,
      destination: Int,
      coords: Int,
      replay: Int,
      gameResult: Int,
      coordSystem: Int,
      zen: Option[Int],
      resizeHandle: Option[Int],
      blindfold: Int
  )

  case class BehaviorData(
      moveEvent: Option[Int],
      premove: Int,
      takeback: Int,
      autoThreefold: Int,
      submitMove: Int,
      confirmResign: Int,
      keyboardMove: Option[Int],
      fullCapture: Option[Int]
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
      insightShare: Int
  ) {

    def apply(pref: Pref) = pref.copy(
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
      gameResult = display.gameResult,
      coordSystem = display.coordSystem,
      blindfold = display.blindfold,
      challenge = challenge,
      message = message,
      studyInvite = studyInvite | Pref.default.studyInvite,
      premove = behavior.premove == 1,
      animation = display.animation,
      submitMove = behavior.submitMove,
      insightShare = insightShare,
      confirmResign = behavior.confirmResign,
      captured = display.captured == 1,
      kingMoves = display.kingMoves == 1,
      keyboardMove = behavior.keyboardMove | pref.keyboardMove,
      fullCapture = behavior.fullCapture | pref.fullCapture,
      zen = display.zen | pref.zen,
      resizeHandle = display.resizeHandle | pref.resizeHandle,
      moveEvent = behavior.moveEvent | pref.moveEvent
    )
  }

  object PrefData {
    def apply(pref: Pref): PrefData = PrefData(
      display = DisplayData(
        highlight = if (pref.highlight) 1 else 0,
        destination = if (pref.destination) 1 else 0,
        animation = pref.animation,
        coords = pref.coords,
        replay = pref.replay,
        gameResult = pref.gameResult,
        coordSystem = pref.coordSystem,
        captured = if (pref.captured) 1 else 0,
        kingMoves = if (pref.kingMoves) 1 else 0,
        blindfold = pref.blindfold,
        zen = pref.zen.some,
        resizeHandle = pref.resizeHandle.some
      ),
      behavior = BehaviorData(
        moveEvent = pref.moveEvent.some,
        premove = if (pref.premove) 1 else 0,
        takeback = pref.takeback,
        autoThreefold = pref.autoThreefold,
        submitMove = pref.submitMove,
        confirmResign = pref.confirmResign,
        keyboardMove = pref.keyboardMove.some,
        fullCapture = pref.fullCapture.some
      ),
      clock = ClockData(
        tenths = pref.clockTenths,
        bar = if (pref.clockBar) 1 else 0,
        sound = if (pref.clockSound) 1 else 0,
        moretime = pref.moretime
      ),
      follow = if (pref.follow) 1 else 0,
      challenge = pref.challenge,
      message = pref.message,
      studyInvite = pref.studyInvite.some,
      insightShare = pref.insightShare
    )
  }

  def prefOf(p: Pref): Form[PrefData] = pref fill PrefData(p)

  val theme = Form(single(
    "theme" -> text.verifying(Theme contains _)
  ))

  val pieceSet = Form(single(
    "set" -> text.verifying(PieceSet contains _)
  ))

  val soundSet = Form(single(
    "set" -> text.verifying(SoundSet contains _)
  ))

  val bg = Form(single(
    "bg" -> text.verifying(List("light", "dark", "transp") contains _)
  ))

  val bgImg = Form(single(
    "bgImg" -> nonEmptyText
  ))

  val zen = Form(single(
    "zen" -> text.verifying(Set("0", "1") contains _)
  ))

  val puzzleVariant = Form(single(
    "puzzleVariant" -> text.verifying(key => draughts.variant.Variant(key).fold(false)(Pref.puzzleVariants contains _))
  ))

}

package lila.pref

import play.api.data._
import play.api.data.Forms._

object DataForm {

  val pref = Form(mapping(
    "display" -> mapping(
      "animation" -> number.verifying(Set(0, 1, 2, 3) contains _),
      "captured" -> number.verifying(Pref.BooleanPref.verify),
      "highlight" -> number.verifying(Pref.BooleanPref.verify),
      "destination" -> number.verifying(Pref.BooleanPref.verify),
      "coords" -> number.verifying(Pref.Coords.choices.toMap contains _),
      "replay" -> number.verifying(Pref.Replay.choices.toMap contains _),
      "pieceNotation" -> optional(number.verifying(Pref.BooleanPref.verify)),
      "zen" -> optional(number.verifying(Pref.BooleanPref.verify)),
      "blindfold" -> number.verifying(Pref.Blindfold.choices.toMap contains _)
    )(DisplayData.apply)(DisplayData.unapply),
    "behavior" -> mapping(
      "moveEvent" -> optional(number.verifying(Set(0, 1, 2) contains _)),
      "premove" -> number.verifying(Pref.BooleanPref.verify),
      "takeback" -> number.verifying(Pref.Takeback.choices.toMap contains _),
      "autoQueen" -> number.verifying(Pref.AutoQueen.choices.toMap contains _),
      "autoThreefold" -> number.verifying(Pref.AutoThreefold.choices.toMap contains _),
      "submitMove" -> number.verifying(Pref.SubmitMove.choices.toMap contains _),
      "confirmResign" -> number.verifying(Pref.ConfirmResign.choices.toMap contains _),
      "keyboardMove" -> optional(number.verifying(Pref.BooleanPref.verify)),
      "rookCastle" -> optional(number.verifying(Pref.BooleanPref.verify))
    )(BehaviorData.apply)(BehaviorData.unapply),
    "clockTenths" -> number.verifying(Pref.ClockTenths.choices.toMap contains _),
    "clockBar" -> number.verifying(Pref.BooleanPref.verify),
    "clockSound" -> number.verifying(Pref.BooleanPref.verify),
    "follow" -> number.verifying(Pref.BooleanPref.verify),
    "challenge" -> number.verifying(Pref.Challenge.choices.toMap contains _),
    "message" -> number.verifying(Pref.Message.choices.toMap contains _),
    "studyInvite" -> optional(number.verifying(Pref.StudyInvite.choices.toMap contains _)),
    "insightShare" -> number.verifying(Set(0, 1, 2) contains _)
  )(PrefData.apply)(PrefData.unapply))

  case class DisplayData(
      animation: Int,
      captured: Int,
      highlight: Int,
      destination: Int,
      coords: Int,
      replay: Int,
      pieceNotation: Option[Int],
      zen: Option[Int],
      blindfold: Int
  )

  case class BehaviorData(
      moveEvent: Option[Int],
      premove: Int,
      takeback: Int,
      autoQueen: Int,
      autoThreefold: Int,
      submitMove: Int,
      confirmResign: Int,
      keyboardMove: Option[Int],
      rookCastle: Option[Int]
  )

  case class PrefData(
      display: DisplayData,
      behavior: BehaviorData,
      clockTenths: Int,
      clockBar: Int,
      clockSound: Int,
      follow: Int,
      challenge: Int,
      message: Int,
      studyInvite: Option[Int],
      insightShare: Int
  ) {

    def apply(pref: Pref) = pref.copy(
      autoQueen = behavior.autoQueen,
      autoThreefold = behavior.autoThreefold,
      takeback = behavior.takeback,
      clockTenths = clockTenths,
      clockBar = clockBar == 1,
      clockSound = clockSound == 1,
      follow = follow == 1,
      highlight = display.highlight == 1,
      destination = display.destination == 1,
      coords = display.coords,
      replay = display.replay,
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
      keyboardMove = behavior.keyboardMove | pref.keyboardMove,
      zen = display.zen | pref.zen,
      rookCastle = behavior.rookCastle | pref.rookCastle,
      pieceNotation = display.pieceNotation | pref.pieceNotation,
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
        captured = if (pref.captured) 1 else 0,
        blindfold = pref.blindfold,
        zen = pref.zen.some,
        pieceNotation = pref.pieceNotation.some
      ),
      behavior = BehaviorData(
        moveEvent = pref.moveEvent.some,
        premove = if (pref.premove) 1 else 0,
        takeback = pref.takeback,
        autoQueen = pref.autoQueen,
        autoThreefold = pref.autoThreefold,
        submitMove = pref.submitMove,
        confirmResign = pref.confirmResign,
        keyboardMove = pref.keyboardMove.some,
        rookCastle = pref.rookCastle.some
      ),
      clockTenths = pref.clockTenths,
      clockBar = if (pref.clockBar) 1 else 0,
      clockSound = if (pref.clockSound) 1 else 0,
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

  val theme3d = Form(single(
    "theme" -> text.verifying(Theme3d contains _)
  ))

  val pieceSet3d = Form(single(
    "set" -> text.verifying(PieceSet3d contains _)
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

  val is3d = Form(single(
    "is3d" -> text.verifying(List("true", "false") contains _)
  ))

  val zen = Form(single(
    "zen" -> text.verifying(Set("0", "1") contains _)
  ))
}

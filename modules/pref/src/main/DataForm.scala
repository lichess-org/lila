package lila.pref

import play.api.data._
import play.api.data.Forms._

import lila.user.User

object DataForm {

  val pref = Form(mapping(
    "display" -> mapping(
      "animation" -> number.verifying(Set(0, 1, 2, 3) contains _),
      "captured" -> number.verifying(Set(0, 1) contains _),
      "highlight" -> number.verifying(Set(0, 1) contains _),
      "destination" -> number.verifying(Set(0, 1) contains _),
      "coords" -> number.verifying(Pref.Coords.choices.toMap contains _),
      "replay" -> number.verifying(Pref.Replay.choices.toMap contains _),
      "pieceNotation" -> optional(number.verifying(Set(0, 1) contains _)),
      "blindfold" -> number.verifying(Pref.Blindfold.choices.toMap contains _)
    )(DisplayData.apply)(DisplayData.unapply),
    "autoQueen" -> number.verifying(Pref.AutoQueen.choices.toMap contains _),
    "autoThreefold" -> number.verifying(Pref.AutoThreefold.choices.toMap contains _),
    "takeback" -> number.verifying(Pref.Takeback.choices.toMap contains _),
    "clockTenths" -> number.verifying(Pref.ClockTenths.choices.toMap contains _),
    "clockBar" -> number.verifying(Set(0, 1) contains _),
    "clockSound" -> number.verifying(Set(0, 1) contains _),
    "follow" -> number.verifying(Set(0, 1) contains _),
    "challenge" -> number.verifying(Pref.Challenge.choices.toMap contains _),
    "message" -> number.verifying(Pref.Message.choices.toMap contains _),
    "premove" -> number.verifying(Set(0, 1) contains _),
    "submitMove" -> number.verifying(Pref.SubmitMove.choices.toMap contains _),
    "insightShare" -> number.verifying(Set(0, 1, 2) contains _),
    "confirmResign" -> number.verifying(Pref.ConfirmResign.choices.toMap contains _),
    // new preferences must be optional for mobile app BC
    "keyboardMove" -> optional(number.verifying(Set(0, 1) contains _)),
    "moveEvent" -> optional(number.verifying(Set(0, 1, 2) contains _))
  )(PrefData.apply)(PrefData.unapply))

  case class DisplayData(
    animation: Int,
    captured: Int,
    highlight: Int,
    destination: Int,
    coords: Int,
    replay: Int,
    pieceNotation: Option[Int],
    blindfold: Int)

  case class PrefData(
      display: DisplayData,
      autoQueen: Int,
      autoThreefold: Int,
      takeback: Int,
      clockTenths: Int,
      clockBar: Int,
      clockSound: Int,
      follow: Int,
      challenge: Int,
      message: Int,
      premove: Int,
      submitMove: Int,
      insightShare: Int,
      confirmResign: Int,
      keyboardMove: Option[Int],
      moveEvent: Option[Int]) {

    def apply(pref: Pref) = pref.copy(
      autoQueen = autoQueen,
      autoThreefold = autoThreefold,
      takeback = takeback,
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
      premove = premove == 1,
      animation = display.animation,
      submitMove = submitMove,
      insightShare = insightShare,
      confirmResign = confirmResign,
      captured = display.captured == 1,
      keyboardMove = keyboardMove | pref.keyboardMove,
      pieceNotation = display.pieceNotation | pref.pieceNotation,
      moveEvent = moveEvent | pref.moveEvent)
  }

  object PrefData {
    def apply(pref: Pref): PrefData = PrefData(
      display = DisplayData(
        highlight = pref.highlight.fold(1, 0),
        destination = pref.destination.fold(1, 0),
        animation = pref.animation,
        coords = pref.coords,
        replay = pref.replay,
        captured = pref.captured.fold(1, 0),
        blindfold = pref.blindfold,
        pieceNotation = pref.pieceNotation.some),
      autoQueen = pref.autoQueen,
      autoThreefold = pref.autoThreefold,
      takeback = pref.takeback,
      clockTenths = pref.clockTenths,
      clockBar = pref.clockBar.fold(1, 0),
      clockSound = pref.clockSound.fold(1, 0),
      follow = pref.follow.fold(1, 0),
      challenge = pref.challenge,
      message = pref.message,
      premove = pref.premove.fold(1, 0),
      submitMove = pref.submitMove,
      insightShare = pref.insightShare,
      confirmResign = pref.confirmResign,
      keyboardMove = pref.keyboardMove.some,
      moveEvent = pref.moveEvent.some)
  }

  def prefOf(p: Pref): Form[PrefData] = pref fill PrefData(p)

  val theme = Form(single(
    "theme" -> nonEmptyText.verifying(Theme contains _)
  ))

  val pieceSet = Form(single(
    "set" -> nonEmptyText.verifying(PieceSet contains _)
  ))

  val theme3d = Form(single(
    "theme" -> nonEmptyText.verifying(Theme3d contains _)
  ))

  val pieceSet3d = Form(single(
    "set" -> nonEmptyText.verifying(PieceSet3d contains _)
  ))

  val soundSet = Form(single(
    "set" -> nonEmptyText.verifying(SoundSet contains _)
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
}

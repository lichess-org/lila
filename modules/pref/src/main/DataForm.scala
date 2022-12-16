package lila.pref

import play.api.data._
import play.api.data.Forms._

object DataForm {

  private def containedIn(choices: Seq[(Int, String)]): Int => Boolean =
    choice => choices.exists(_._1 == choice)

  private def checkedNumber(choices: Seq[(Int, String)]) =
    number.verifying(containedIn(choices))

  private lazy val booleanNumber =
    number.verifying(Pref.BooleanPref.verify)

  val pref = Form(
    mapping(
      "display" -> mapping(
        "boardLayout"        -> checkedNumber(Pref.BoardLayout.choices),
        "animation"          -> checkedNumber(Pref.Animation.choices),
        "coords"             -> checkedNumber(Pref.Coords.choices),
        "highlightLastDests" -> booleanNumber,
        "highlightCheck"     -> booleanNumber,
        "squareOverlay"      -> booleanNumber,
        "destination"        -> booleanNumber,
        "dropDestination"    -> booleanNumber,
        "replay"             -> checkedNumber(Pref.Replay.choices),
        "zen"                -> optional(booleanNumber),
        "resizeHandle"       -> optional(checkedNumber(Pref.ResizeHandle.choices)),
        "blindfold"          -> checkedNumber(Pref.Blindfold.choices)
      )(DisplayData.apply)(DisplayData.unapply),
      "behavior" -> mapping(
        "moveEvent"     -> optional(checkedNumber(Pref.MoveEvent.choices)),
        "premove"       -> booleanNumber,
        "takeback"      -> checkedNumber(Pref.Takeback.choices),
        "submitMove"    -> checkedNumber(Pref.SubmitMove.choices),
        "confirmResign" -> checkedNumber(Pref.ConfirmResign.choices),
        "keyboardMove"  -> optional(booleanNumber)
      )(BehaviorData.apply)(BehaviorData.unapply),
      "clock" -> mapping(
        "tenths"    -> checkedNumber(Pref.ClockTenths.choices),
        "countdown" -> checkedNumber(Pref.ClockCountdown.choices),
        "sound"     -> booleanNumber,
        "moretime"  -> checkedNumber(Pref.Moretime.choices)
      )(ClockData.apply)(ClockData.unapply),
      "follow"       -> booleanNumber,
      "challenge"    -> checkedNumber(Pref.Challenge.choices),
      "message"      -> checkedNumber(Pref.Message.choices),
      "studyInvite"  -> optional(checkedNumber(Pref.StudyInvite.choices)),
      "insightShare" -> checkedNumber(Pref.InsightShare.choices)
    )(PrefData.apply)(PrefData.unapply)
  )

  case class DisplayData(
      boardLayout: Int,
      animation: Int,
      coords: Int,
      highlightLastDests: Int,
      highlightCheck: Int,
      squareOverlay: Int,
      destination: Int,
      dropDestination: Int,
      replay: Int,
      zen: Option[Int],
      resizeHandle: Option[Int],
      blindfold: Int
  )

  case class BehaviorData(
      moveEvent: Option[Int],
      premove: Int,
      takeback: Int,
      submitMove: Int,
      confirmResign: Int,
      keyboardMove: Option[Int]
  )

  case class ClockData(
      tenths: Int,
      countdown: Int,
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

    def apply(pref: Pref) =
      pref.copy(
        takeback = behavior.takeback,
        moretime = clock.moretime,
        clockTenths = clock.tenths,
        clockCountdown = clock.countdown,
        clockSound = clock.sound == 1,
        follow = follow == 1,
        highlightLastDests = display.highlightLastDests == 1,
        highlightCheck = display.highlightCheck == 1,
        squareOverlay = display.squareOverlay == 1,
        destination = display.destination == 1,
        dropDestination = display.dropDestination == 1,
        coords = display.coords,
        replay = display.replay,
        blindfold = display.blindfold,
        challenge = challenge,
        message = message,
        studyInvite = studyInvite | Pref.default.studyInvite,
        premove = behavior.premove == 1,
        boardLayout = display.boardLayout,
        animation = display.animation,
        submitMove = behavior.submitMove,
        insightShare = insightShare,
        confirmResign = behavior.confirmResign,
        keyboardMove = behavior.keyboardMove | pref.keyboardMove,
        zen = display.zen | pref.zen,
        resizeHandle = display.resizeHandle | pref.resizeHandle,
        moveEvent = behavior.moveEvent | pref.moveEvent
      )
  }

  object PrefData {
    def apply(pref: Pref): PrefData =
      PrefData(
        display = DisplayData(
          coords = pref.coords,
          highlightLastDests = if (pref.highlightLastDests) 1 else 0,
          highlightCheck = if (pref.highlightCheck) 1 else 0,
          squareOverlay = if (pref.squareOverlay) 1 else 0,
          destination = if (pref.destination) 1 else 0,
          dropDestination = if (pref.dropDestination) 1 else 0,
          boardLayout = pref.boardLayout,
          animation = pref.animation,
          replay = pref.replay,
          blindfold = pref.blindfold,
          zen = pref.zen.some,
          resizeHandle = pref.resizeHandle.some
        ),
        behavior = BehaviorData(
          moveEvent = pref.moveEvent.some,
          premove = if (pref.premove) 1 else 0,
          takeback = pref.takeback,
          submitMove = pref.submitMove,
          confirmResign = pref.confirmResign,
          keyboardMove = pref.keyboardMove.some
        ),
        clock = ClockData(
          tenths = pref.clockTenths,
          countdown = pref.clockCountdown,
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

  val theme = Form(
    single(
      "theme" -> text.verifying(Theme contains _)
    )
  )

  val pieceSet = Form(
    single(
      "set" -> text.verifying(PieceSet contains _)
    )
  )

  val chuPieceSet = Form(
    single(
      "set" -> text.verifying(ChuPieceSet contains _)
    )
  )

  val soundSet = Form(
    single(
      "set" -> text.verifying(SoundSet contains _)
    )
  )

  val bg = Form(
    single(
      "bg" -> text.verifying(List("light", "dark", "transp") contains _)
    )
  )

  val bgImg = Form(
    single(
      "bgImg" -> text.verifying { url =>
        url.getBytes("UTF-8").length < 400 && (url.isEmpty || url.startsWith("https://") || url.startsWith(
          "//"
        ))
      }
    )
  )

  val zen = Form(
    single(
      "zen" -> text.verifying(Set("0", "1") contains _)
    )
  )

  val notation = Form(
    single(
      "notation" -> text.verifying(Set("0", "1", "2", "3") contains _)
    )
  )

  val customTheme = Form(
    mapping(
      "boardColor" -> text(maxLength = 30),
      "boardImg" -> text.verifying { url =>
        url.getBytes("UTF-8").length < 400 && (url.isEmpty || url.startsWith("https://") || url.startsWith(
          "//"
        ))
      },
      "gridColor"  -> text(maxLength = 30),
      "gridWidth"  -> number.verifying(Set(0, 1, 2, 3) contains _),
      "handsColor" -> text(maxLength = 30),
      "handsImg" -> text.verifying { url =>
        url.getBytes("UTF-8").length < 400 && (url.isEmpty || url.startsWith("https://") || url.startsWith(
          "//"
        ))
      }
    )(CustomTheme.apply)(CustomTheme.unapply)
  )

}

package lidraughts.pref

import draughts.variant.{ Variant, Standard, Frisian, Russian }

case class Pref(
    _id: String, // user id
    dark: Boolean,
    transp: Boolean,
    bgImg: Option[String],
    theme: String,
    pieceSet: String,
    soundSet: String,
    blindfold: Int,
    autoThreefold: Int,
    takeback: Int,
    moretime: Int,
    clockTenths: Int,
    clockBar: Boolean,
    clockSound: Boolean,
    premove: Boolean,
    animation: Int,
    captured: Boolean,
    kingMoves: Boolean,
    follow: Boolean,
    highlight: Boolean,
    destination: Boolean,
    coords: Int,
    replay: Int,
    gameResult: Int,
    coordSystem: Int,
    challenge: Int,
    message: Int,
    studyInvite: Int,
    coordColor: Int,
    submitMove: Int,
    confirmResign: Int,
    insightShare: Int,
    keyboardMove: Int,
    fullCapture: Int,
    zen: Int,
    moveEvent: Int,
    puzzleVariant: Variant,
    resizeHandle: Int,
    tags: Map[String, String] = Map.empty
) {

  import Pref._

  def id = _id

  def realTheme = Theme(theme)
  def realPieceSet = PieceSet(pieceSet)
  def realSoundSet = SoundSet(soundSet)

  def coordColorName = Color.choices.toMap.get(coordColor).fold("random")(_.toLowerCase)
  def coordsClass = Coords classOf coords

  def hasSeenVerifyTitle = tags contains Tag.verifyTitle

  def set(name: String, value: String): Option[Pref] = name match {
    case "bg" =>
      if (value == "transp") copy(dark = true, transp = true).some
      else copy(dark = value == "dark", transp = false).some
    case "bgImg" => copy(bgImg = value.some).some
    case "theme" => Theme.allByName get value map { t => copy(theme = t.name) }
    case "pieceSet" => PieceSet.allByName get value map { p => copy(pieceSet = p.name) }
    case "soundSet" => SoundSet.allByKey get value map { s => copy(soundSet = s.name) }
    case "zen" => copy(zen = if (value == "1") 1 else 0).some
    case "puzzleVariant" => copy(puzzleVariant = Variant(value).getOrElse(Standard)).some
    case _ => none
  }

  def animationFactor = animation match {
    case Animation.NONE => 0.0f
    case Animation.FAST => 0.5f
    case Animation.NORMAL => 1.0f
    case Animation.SLOW => 2.0f
    case _ => 1.0f
  }

  def draughtsResult = gameResult == Pref.GameResult.DRAUGHTS

  def isAlgebraic(v: Variant) = canAlgebraic && v.boardSize.pos.hasAlgebraic
  def canAlgebraic = coordSystem == Pref.CoordSystem.ALGEBRAIC

  def isBlindfold = blindfold == Pref.Blindfold.YES

  def bgImgOrDefault = bgImg | Pref.defaultBgImg

  def isZen = zen == Zen.YES

  // atob("aHR0cDovL2NoZXNzLWNoZWF0LmNvbS9ob3dfdG9fY2hlYXRfYXRfbGljaGVzcy5odG1s")
  def botCompatible =
    theme == "brown" &&
      pieceSet == "wide_crown" &&
      animation == Animation.NONE &&
      highlight &&
      coords == Coords.OUTSIDE
}

object Pref {

  val defaultBgImg = "/assets/images/background/wood.jpg"

  trait BooleanPref {
    val NO = 0
    val YES = 1
    val choices = Seq(NO -> "No", YES -> "Yes")
  }

  object BooleanPref {
    val verify = (v: Int) => v == 0 || v == 1
  }

  object Tag {
    val verifyTitle = "verifyTitle"
  }

  object Color {
    val WHITE = 1
    val RANDOM = 2
    val BLACK = 3

    val choices = Seq(
      WHITE -> "White",
      RANDOM -> "Random",
      BLACK -> "Black"
    )
  }

  object SubmitMove {
    val NEVER = 0
    val CORRESPONDENCE_ONLY = 4
    val CORRESPONDENCE_UNLIMITED = 1
    val ALWAYS = 2

    val choices = Seq(
      NEVER -> "Never",
      CORRESPONDENCE_ONLY -> "Correspondence games only",
      CORRESPONDENCE_UNLIMITED -> "Correspondence and unlimited",
      ALWAYS -> "Always"
    )
  }

  object ConfirmResign extends BooleanPref

  object InsightShare {
    val NOBODY = 0
    val FRIENDS = 1
    val EVERYBODY = 2

    val choices = Seq(
      NOBODY -> "With nobody",
      FRIENDS -> "With friends",
      EVERYBODY -> "With everybody"
    )
  }

  object KeyboardMove extends BooleanPref

  object FullCapture extends BooleanPref

  object MoveEvent {
    val CLICK = 0
    val DRAG = 1
    val BOTH = 2

    val choices = Seq(
      CLICK -> "Click two squares",
      DRAG -> "Drag a piece",
      BOTH -> "Both clicks and drag"
    )
  }

  object Blindfold extends BooleanPref {
    override val choices = Seq(
      NO -> "What? No!",
      YES -> "Yes, hide the pieces"
    )
  }

  object AutoThreefold {
    val NEVER = 1
    val TIME = 2
    val ALWAYS = 3

    val choices = Seq(
      NEVER -> "Never",
      ALWAYS -> "Always",
      TIME -> "When time remaining < 30 seconds"
    )
  }

  object Takeback {
    val NEVER = 1
    val CASUAL = 2
    val ALWAYS = 3

    val choices = Seq(
      NEVER -> "Never",
      ALWAYS -> "Always",
      CASUAL -> "In casual games only"
    )
  }

  object Moretime {
    val NEVER = 1
    val CASUAL = 2
    val ALWAYS = 3

    val choices = Seq(
      NEVER -> "Never",
      ALWAYS -> "Always",
      CASUAL -> "In casual games only"
    )
  }

  object Animation {
    val NONE = 0
    val FAST = 1
    val NORMAL = 2
    val SLOW = 3

    val choices = Seq(
      NONE -> "None",
      FAST -> "Fast",
      NORMAL -> "Normal",
      SLOW -> "Slow"
    )
  }

  object Coords {
    val NONE = 0
    val INSIDE = 1
    val OUTSIDE = 2

    val choices = Seq(
      NONE -> "No",
      INSIDE -> "Inside the board",
      OUTSIDE -> "Outside the board"
    )

    def classOf(v: Int) = v match {
      case INSIDE => "in"
      case OUTSIDE => "out"
      case _ => "no"
    }
  }

  object Replay {
    val NEVER = 0
    val SLOW = 1
    val ALWAYS = 2

    val choices = Seq(
      NEVER -> "Never",
      SLOW -> "On slow games",
      ALWAYS -> "Always"
    )
  }

  object GameResult {
    val STANDARD = 0
    val DRAUGHTS = 1

    val choices = Seq(
      STANDARD -> "1-0 • ½-½ • 0-1",
      DRAUGHTS -> "2-0 • 1-1 • 0-2"
    )
  }

  object CoordSystem {
    val FIELDNUMBERS = 0
    val ALGEBRAIC = 1

    val choices = Seq(
      FIELDNUMBERS -> "Fieldnumbers",
      ALGEBRAIC -> "Algebraic"
    )
  }

  object ClockTenths {
    val NEVER = 0
    val LOWTIME = 1
    val ALWAYS = 2

    val choices = Seq(
      NEVER -> "Never",
      LOWTIME -> "When time remaining < 10 seconds",
      ALWAYS -> "Always"
    )
  }

  object Challenge {
    val NEVER = 1
    val RATING = 2
    val FRIEND = 3
    val ALWAYS = 4

    val ratingThreshold = 300

    val choices = Seq(
      NEVER -> "Never",
      RATING -> s"If rating is ± $ratingThreshold",
      FRIEND -> "Only friends",
      ALWAYS -> "Always"
    )
  }

  object Message {
    val NEVER = 1
    val FRIEND = 2
    val ALWAYS = 3

    val choices = Seq(
      NEVER -> "Never",
      FRIEND -> "Only friends",
      ALWAYS -> "Always"
    )
  }

  object StudyInvite {
    val NEVER = 1
    val FRIEND = 2
    val ALWAYS = 3

    val choices = Seq(
      NEVER -> "Never",
      FRIEND -> "Only friends",
      ALWAYS -> "Always"
    )
  }

  object ResizeHandle {
    val NEVER = 0
    val INITIAL = 1
    val ALWAYS = 2

    val choices = Seq(
      NEVER -> "Never",
      INITIAL -> "On initial position",
      ALWAYS -> "Always"
    )
  }

  object Zen extends BooleanPref {
  }

  val puzzleVariants: List[Variant] = List(Standard, Frisian, Russian)

  def create(id: String) = default.copy(_id = id)

  lazy val default = Pref(
    _id = "",
    dark = false,
    transp = false,
    bgImg = none,
    theme = Theme.default.name,
    pieceSet = PieceSet.default.name,
    soundSet = SoundSet.default.name,
    blindfold = Blindfold.NO,
    autoThreefold = AutoThreefold.TIME,
    takeback = Takeback.ALWAYS,
    moretime = Moretime.ALWAYS,
    clockBar = true,
    clockSound = true,
    premove = true,
    animation = 2,
    captured = true,
    kingMoves = true,
    fullCapture = FullCapture.NO,
    follow = true,
    highlight = true,
    destination = true,
    coords = Coords.OUTSIDE,
    replay = Replay.ALWAYS,
    gameResult = GameResult.DRAUGHTS,
    coordSystem = CoordSystem.FIELDNUMBERS,
    clockTenths = ClockTenths.LOWTIME,
    challenge = Challenge.ALWAYS,
    message = Message.ALWAYS,
    studyInvite = StudyInvite.ALWAYS,
    coordColor = Color.RANDOM,
    submitMove = SubmitMove.CORRESPONDENCE_ONLY,
    confirmResign = ConfirmResign.YES,
    insightShare = InsightShare.FRIENDS,
    keyboardMove = KeyboardMove.NO,
    zen = Zen.NO,
    moveEvent = MoveEvent.BOTH,
    puzzleVariant = Standard,
    resizeHandle = ResizeHandle.INITIAL,
    tags = Map.empty
  )

  import ornicar.scalalib.Zero
  implicit def PrefZero: Zero[Pref] = Zero.instance(default)
}

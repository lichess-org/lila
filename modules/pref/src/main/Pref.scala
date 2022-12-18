package lila.pref

import org.joda.time.DateTime

import lila.user.User

case class Pref(
    _id: UserId,
    bg: Int,
    bgImg: Option[String],
    is3d: Boolean,
    theme: String,
    pieceSet: String,
    theme3d: String,
    pieceSet3d: String,
    soundSet: String,
    blindfold: Int,
    autoQueen: Int,
    autoThreefold: Int,
    takeback: Int,
    moretime: Int,
    clockTenths: Int,
    clockBar: Boolean,
    clockSound: Boolean,
    premove: Boolean,
    animation: Int,
    captured: Boolean,
    follow: Boolean,
    highlight: Boolean,
    destination: Boolean,
    coords: Int,
    replay: Int,
    challenge: Int,
    message: Int,
    studyInvite: Int,
    submitMove: Int,
    confirmResign: Int,
    mention: Boolean,
    corresEmailNotif: Boolean,
    insightShare: Int,
    keyboardMove: Int,
    zen: Int,
    ratings: Int,
    rookCastle: Int,
    moveEvent: Int,
    pieceNotation: Int,
    resizeHandle: Int,
    agreement: Int,
    tags: Map[String, String] = Map.empty
):

  import Pref.*

  inline def id = _id

  def realTheme      = Theme(theme)
  def realPieceSet   = PieceSet(pieceSet)
  def realTheme3d    = Theme3d(theme3d)
  def realPieceSet3d = PieceSet3d(pieceSet3d)

  val themeColorLight = "#dbd7d1"
  val themeColorDark  = "#2e2a24"
  def themeColor      = if (bg == Bg.LIGHT) themeColorLight else themeColorDark

  def realSoundSet = SoundSet(soundSet)

  def coordsClass = Coords classOf coords

  def hasDgt = tags contains Tag.dgt

  def set(name: String, value: String): Option[Pref] =
    name match
      case "bg"    => Pref.Bg.fromString.get(value).map { bg => copy(bg = bg) }
      case "bgImg" => copy(bgImg = value.some).some
      case "theme" =>
        Theme.allByName get value map { t =>
          copy(theme = t.name)
        }
      case "pieceSet" =>
        PieceSet.allByName get value map { p =>
          copy(pieceSet = p.name)
        }
      case "theme3d" =>
        Theme3d.allByName get value map { t =>
          copy(theme3d = t.name)
        }
      case "pieceSet3d" =>
        PieceSet3d.allByName get value map { p =>
          copy(pieceSet3d = p.name)
        }
      case "is3d" => copy(is3d = value == "true").some
      case "soundSet" =>
        SoundSet.allByKey get value map { s =>
          copy(soundSet = s.key)
        }
      case "zen" => copy(zen = if (value == "1") 1 else 0).some
      case _     => none

  def animationMillis: Int =
    animation match
      case Animation.NONE   => 0
      case Animation.FAST   => 120
      case Animation.NORMAL => 250
      case Animation.SLOW   => 500
      case _                => 250

  def animationMillisForSpeedPuzzles: Int =
    animation match
      case Animation.NONE => 0
      case Animation.SLOW => 120
      case _              => 70

  def isBlindfold = blindfold == Pref.Blindfold.YES

  def bgImgOrDefault = bgImg | Pref.defaultBgImg

  def pieceNotationIsLetter = pieceNotation == PieceNotation.LETTER

  def isZen = zen == Zen.YES

  val showRatings = ratings == Ratings.YES

  def is2d = !is3d

  def agreementNeededSince: Option[DateTime] =
    Agreement.showPrompt && agreement < Agreement.current option Agreement.changedAt

  def agree = copy(agreement = Agreement.current)

  def hasKeyboardMove = keyboardMove == KeyboardMove.YES

  // atob("aHR0cDovL2NoZXNzLWNoZWF0LmNvbS9ob3dfdG9fY2hlYXRfYXRfbGljaGVzcy5odG1s")
  def botCompatible =
    theme == "brown" &&
      pieceSet == "cburnett" &&
      is2d &&
      animation == Animation.NONE &&
      highlight &&
      coords == Coords.OUTSIDE

object Pref:

  val defaultBgImg = "//lichess1.org/assets/images/background/landscape.jpg"

  trait BooleanPref:
    val NO      = 0
    val YES     = 1
    val choices = Seq(NO -> "No", YES -> "Yes")

  object BooleanPref:
    val verify = (v: Int) => v == 0 || v == 1

  object Bg:
    val LIGHT       = 100
    val DARK        = 200
    val DARKBOARD   = 300
    val TRANSPARENT = 400
    val SYSTEM      = 500

    val choices = Seq(
      LIGHT       -> "Light",
      DARK        -> "Dark",
      DARKBOARD   -> "Dark Board",
      TRANSPARENT -> "Transparent",
      SYSTEM      -> "Device theme"
    )

    val fromString = Map(
      "light"     -> LIGHT,
      "dark"      -> DARK,
      "darkBoard" -> DARKBOARD,
      "transp"    -> TRANSPARENT,
      "system"    -> SYSTEM
    )

    val asString = fromString.map(_.swap)

  object Tag:
    val dgt = "dgt"

  object Color:
    val WHITE  = 1
    val RANDOM = 2
    val BLACK  = 3

    val choices = Seq(
      WHITE  -> "White",
      RANDOM -> "Random",
      BLACK  -> "Black"
    )

  object AutoQueen:
    val NEVER   = 1
    val PREMOVE = 2
    val ALWAYS  = 3

    val choices = Seq(
      NEVER   -> "Never",
      ALWAYS  -> "Always",
      PREMOVE -> "When premoving"
    )

  object SubmitMove:
    val NEVER                    = 0
    val CORRESPONDENCE_ONLY      = 4
    val CORRESPONDENCE_UNLIMITED = 1
    val ALWAYS                   = 2

    val choices = Seq(
      NEVER                    -> "Never",
      CORRESPONDENCE_ONLY      -> "Correspondence games only",
      CORRESPONDENCE_UNLIMITED -> "Correspondence and unlimited",
      ALWAYS                   -> "Always"
    )

  object ConfirmResign extends BooleanPref

  object InsightShare:
    val NOBODY    = 0
    val FRIENDS   = 1
    val EVERYBODY = 2

    val choices = Seq(
      NOBODY    -> "With nobody",
      FRIENDS   -> "With friends",
      EVERYBODY -> "With everybody"
    )

  object Mention extends BooleanPref

  object CorresEmailNotif extends BooleanPref

  object KeyboardMove extends BooleanPref

  object RookCastle:
    val NO  = 0
    val YES = 1

    val choices = Seq(
      NO  -> "Castle by moving by two squares",
      YES -> "Castle by moving onto the rook"
    )

  object MoveEvent:
    val CLICK = 0
    val DRAG  = 1
    val BOTH  = 2

    val choices = Seq(
      CLICK -> "Click two squares",
      DRAG  -> "Drag a piece",
      BOTH  -> "Both clicks and drag"
    )

  object PieceNotation:
    val SYMBOL = 0
    val LETTER = 1

    val choices = Seq(
      SYMBOL -> "Chess piece symbol",
      LETTER -> "PGN letter (K, Q, R, B, N)"
    )

  object Blindfold extends BooleanPref:
    override val choices = Seq(
      NO  -> "What? No!",
      YES -> "Yes, hide the pieces"
    )

  object AutoThreefold:
    val NEVER  = 1
    val TIME   = 2
    val ALWAYS = 3

    val choices = Seq(
      NEVER  -> "Never",
      ALWAYS -> "Always",
      TIME   -> "When time remaining < 30 seconds"
    )

  object Takeback:
    val NEVER  = 1
    val CASUAL = 2
    val ALWAYS = 3

    val choices = Seq(
      NEVER  -> "Never",
      ALWAYS -> "Always",
      CASUAL -> "In casual games only"
    )

  object Moretime:
    val NEVER  = 1
    val CASUAL = 2
    val ALWAYS = 3

    val choices = Seq(
      NEVER  -> "Never",
      ALWAYS -> "Always",
      CASUAL -> "In casual games only"
    )

  object Animation:
    val NONE   = 0
    val FAST   = 1
    val NORMAL = 2
    val SLOW   = 3

    val choices = Seq(
      NONE   -> "None",
      FAST   -> "Fast",
      NORMAL -> "Normal",
      SLOW   -> "Slow"
    )

  object Coords:
    val NONE    = 0
    val INSIDE  = 1
    val OUTSIDE = 2

    val choices = Seq(
      NONE    -> "No",
      INSIDE  -> "Inside the board",
      OUTSIDE -> "Outside the board"
    )

    def classOf(v: Int) =
      v match
        case INSIDE  => "in"
        case OUTSIDE => "out"
        case _       => "no"

  object Replay:
    val NEVER  = 0
    val SLOW   = 1
    val ALWAYS = 2

    val choices = Seq(
      NEVER  -> "Never",
      SLOW   -> "On slow games",
      ALWAYS -> "Always"
    )

  object ClockTenths:
    val NEVER   = 0
    val LOWTIME = 1
    val ALWAYS  = 2

    val choices = Seq(
      NEVER   -> "Never",
      LOWTIME -> "When time remaining < 10 seconds",
      ALWAYS  -> "Always"
    )

  object Challenge:
    val NEVER      = 1
    val RATING     = 2
    val FRIEND     = 3
    val REGISTERED = 4
    val ALWAYS     = 5

    val ratingThreshold = 300

    val choices = Seq(
      NEVER      -> "Never",
      RATING     -> s"If rating is ± $ratingThreshold",
      FRIEND     -> "Only friends",
      REGISTERED -> "If registered",
      ALWAYS     -> "Always"
    )

  object Message:
    val NEVER  = 1
    val FRIEND = 2
    val ALWAYS = 3

    val choices = Seq(
      NEVER  -> "Only existing conversations",
      FRIEND -> "Only friends",
      ALWAYS -> "Always"
    )

  object StudyInvite:
    val NEVER  = 1
    val FRIEND = 2
    val ALWAYS = 3

    val choices = Seq(
      NEVER  -> "Never",
      FRIEND -> "Only friends",
      ALWAYS -> "Always"
    )

  object ResizeHandle:
    val NEVER   = 0
    val INITIAL = 1
    val ALWAYS  = 2

    val choices = Seq(
      NEVER   -> "Never",
      INITIAL -> "On initial position",
      ALWAYS  -> "Always"
    )

  object Agreement:
    val current    = 2
    val changedAt  = new DateTime(2021, 12, 28, 8, 0)
    val showPrompt = changedAt.isAfter(DateTime.now minusMonths 6)

  object Zen     extends BooleanPref {}
  object Ratings extends BooleanPref {}

  val darkByDefaultSince   = new DateTime(2021, 11, 7, 8, 0)
  val systemByDefaultSince = new DateTime(2022, 12, 23, 8, 0)

  def create(id: UserId) = default.copy(_id = id)

  def create(user: User) = default.copy(
    _id = user.id,
    bg =
      if user.createdAt.isAfter(systemByDefaultSince) then Bg.SYSTEM
      else if user.createdAt.isAfter(darkByDefaultSince) then Bg.DARK
      else Bg.LIGHT,
    agreement = if (user.createdAt isAfter Agreement.changedAt) Agreement.current else 0
  )

  lazy val default = Pref(
    _id = UserId(""),
    bg = Bg.DARK,
    bgImg = none,
    is3d = false,
    theme = Theme.default.name,
    pieceSet = PieceSet.default.name,
    theme3d = Theme3d.default.name,
    pieceSet3d = PieceSet3d.default.name,
    soundSet = SoundSet.default.name,
    blindfold = Blindfold.NO,
    autoQueen = AutoQueen.PREMOVE,
    autoThreefold = AutoThreefold.ALWAYS,
    takeback = Takeback.ALWAYS,
    moretime = Moretime.ALWAYS,
    clockBar = true,
    clockSound = true,
    premove = true,
    animation = Animation.NORMAL,
    captured = true,
    follow = true,
    highlight = true,
    destination = true,
    coords = Coords.INSIDE,
    replay = Replay.ALWAYS,
    clockTenths = ClockTenths.LOWTIME,
    challenge = Challenge.REGISTERED,
    message = Message.ALWAYS,
    studyInvite = StudyInvite.ALWAYS,
    submitMove = SubmitMove.CORRESPONDENCE_ONLY,
    confirmResign = ConfirmResign.YES,
    mention = true,
    corresEmailNotif = false,
    insightShare = InsightShare.FRIENDS,
    keyboardMove = KeyboardMove.NO,
    zen = Zen.NO,
    ratings = Ratings.YES,
    rookCastle = RookCastle.YES,
    moveEvent = MoveEvent.BOTH,
    pieceNotation = PieceNotation.SYMBOL,
    resizeHandle = ResizeHandle.INITIAL,
    agreement = Agreement.current,
    tags = Map.empty
  )

  import alleycats.Zero
  implicit def PrefZero: Zero[Pref] = Zero(default)

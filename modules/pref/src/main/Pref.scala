package lila.pref

case class Pref(
    _id: String, // user id
    dark: Boolean,
    transp: Boolean,
    bgImg: Option[String],
    theme: String,
    customTheme: Option[CustomTheme],
    pieceSet: String,
    chuPieceSet: String,
    kyoPieceSet: String,
    soundSet: String,
    blindfold: Int,
    takeback: Int,
    moretime: Int,
    clockTenths: Int,
    clockCountdown: Int,
    clockSound: Boolean,
    premove: Boolean,
    boardLayout: Int,
    animation: Int,
    coords: Int,
    clearHands: Boolean,
    handsBackground: Boolean,
    follow: Boolean,
    highlightLastDests: Boolean,
    highlightCheck: Boolean,
    squareOverlay: Boolean,
    destination: Boolean,
    dropDestination: Boolean,
    replay: Int,
    colorName: Int,
    challenge: Int,
    message: Int,
    studyInvite: Int,
    coordColor: Int,
    submitMove: Int,
    confirmResign: Int,
    insightsShare: Boolean,
    thickGrid: Int,
    keyboardMove: Int,
    zen: Int,
    moveEvent: Int,
    notation: Int,
    resizeHandle: Int,
    tags: Map[String, String] = Map.empty
) {

  import Pref._

  def id = _id

  def isUsingCustomTheme = theme == "custom"

  def themeColor = if (transp || dark) "#2e2a24" else "#dbd7d1"

  def coordColorName = Color name coordColor
  def coordsClass    = Coords cssClassOf coords

  def hasSeenVerifyTitle = tags contains Tag.verifyTitle

  def set(name: String, value: String): Option[Pref] =
    name match {
      case "bg" =>
        if (value == "transp") copy(dark = true, transp = true).some
        else copy(dark = value == "dark", transp = false).some
      case "bgImg" => copy(bgImg = value.some).some
      case "theme" =>
        Theme.allByKey get value map { t =>
          copy(theme = t.key)
        }
      case "pieceSet" =>
        PieceSet.allByKey get value map { p =>
          copy(pieceSet = p.key)
        }
      case "chuPieceSet" =>
        ChuPieceSet.allByKey get value map { p =>
          copy(chuPieceSet = p.key)
        }
      case "kyoPieceSet" =>
        KyoPieceSet.allByKey get value map { p =>
          copy(kyoPieceSet = p.key)
        }
      case "soundSet" =>
        SoundSet.allByKey get value map { s =>
          copy(soundSet = s.key)
        }
      case "thickGrid" => copy(thickGrid = if (value == "1") 1 else 0).some
      case "zen"       => copy(zen = if (value == "1") 1 else 0).some
      case "notation" =>
        value.toIntOption flatMap { index =>
          Notations.allByIndex get index map { n =>
            copy(notation = n.index)
          }
        }
      case _ => none
    }

  def animationMillis: Int =
    animation match {
      case Animation.NONE   => 0
      case Animation.FAST   => 120
      case Animation.NORMAL => 250
      case Animation.SLOW   => 500
      case _                => 250
    }

  def isBlindfold = blindfold == Pref.Blindfold.YES

  def bgImgOrDefault = bgImg | Pref.defaultBgImg

  def customThemeOrDefault = customTheme | CustomTheme.default

  def isUsingThickGrid = thickGrid == ThickGrid.YES

  def isZen = zen == Zen.YES
}

object Pref {

  val defaultBgImg = "//lishogi1.org/assets/images/background/nature.jpg"

  trait BooleanPref {
    val NO      = 0
    val YES     = 1
    val choices = Seq(NO, YES)
  }

  object BooleanPref {
    val verify = (v: Int) => v == 0 || v == 1
  }

  object Tag {
    val verifyTitle = "verifyTitle"
  }

  object Color {
    val SENTE  = 1
    val RANDOM = 2
    val GOTE   = 3

    def name(v: Int) =
      v match {
        case SENTE => "sente"
        case GOTE  => "gote"
        case _     => "random"
      }

    val choices = Seq(
      SENTE,
      RANDOM,
      GOTE
    )
  }

  object AutoPromote {
    val NEVER   = 1
    val PREMOVE = 2
    val ALWAYS  = 3

    val choices = Seq(
      NEVER,
      ALWAYS,
      PREMOVE
    )
  }

  object SubmitMove {
    val NEVER                    = 0
    val CORRESPONDENCE_ONLY      = 4
    val CORRESPONDENCE_UNLIMITED = 1
    val ALWAYS                   = 2

    val choices = Seq(
      NEVER,
      CORRESPONDENCE_ONLY,
      CORRESPONDENCE_UNLIMITED,
      ALWAYS
    )
  }

  object ConfirmResign extends BooleanPref

  object ThickGrid extends BooleanPref

  object KeyboardMove extends BooleanPref

  object MoveEvent {
    val CLICK = 0
    val DRAG  = 1
    val BOTH  = 2

    val choices = Seq(
      CLICK,
      DRAG,
      BOTH
    )
  }

  object Blindfold extends BooleanPref {
    override val choices = Seq(
      NO,
      YES
    )
  }

  object Takeback {
    val NEVER  = 1
    val CASUAL = 2
    val ALWAYS = 3

    val choices = Seq(
      NEVER,
      ALWAYS,
      CASUAL
    )
  }

  object Moretime {
    val NEVER  = 1
    val CASUAL = 2
    val ALWAYS = 3

    val choices = Seq(
      NEVER,
      ALWAYS,
      CASUAL
    )
  }

  object BoardLayout {
    val DEFAULT = 0
    val COMPACT = 1
    val SMALL   = 2

    val choices = Seq(
      DEFAULT,
      COMPACT,
      SMALL
    )
  }

  object Animation {
    val NONE   = 0
    val FAST   = 1
    val NORMAL = 2
    val SLOW   = 3

    val choices = Seq(
      NONE,
      FAST,
      NORMAL,
      SLOW
    )
  }

  object Coords {
    val NONE    = 0
    val INSIDE  = 1
    val OUTSIDE = 2
    val EDGE    = 3

    val choices = Seq(
      NONE,
      INSIDE,
      OUTSIDE,
      EDGE
    )

    def cssClassOf(v: Int) =
      v match {
        case INSIDE  => "in"
        case OUTSIDE => "out"
        case EDGE    => "edge"
        case _       => "no"
      }
  }

  object Replay {
    val NEVER  = 0
    val SLOW   = 1
    val ALWAYS = 2

    val choices = Seq(
      NEVER,
      SLOW,
      ALWAYS
    )
  }

  object ColorName {
    val LANG    = 0
    val SENTEJP = 1
    val SENTE   = 2
    val BLACK   = 3

    val choices = Seq(
      LANG,
      SENTEJP,
      SENTE,
      BLACK
    )
  }

  object ClockTenths {
    val NEVER   = 0
    val LOWTIME = 1
    val ALWAYS  = 2

    val choices = Seq(
      NEVER,
      LOWTIME,
      ALWAYS
    )
  }

  object ClockCountdown {
    val NEVER = 0
    val THREE = 3
    val FIVE  = 5
    val TEN   = 10

    val choices = Seq(
      NEVER,
      THREE,
      FIVE,
      TEN
    )
  }

  object Challenge {
    val NEVER  = 1
    val RATING = 2
    val FRIEND = 3
    val ALWAYS = 4

    val ratingThreshold = 300

    val choices = Seq(
      NEVER,
      RATING,
      FRIEND,
      ALWAYS
    )
  }

  object Message {
    val NEVER  = 1
    val FRIEND = 2
    val ALWAYS = 3

    val choices = Seq(
      NEVER,
      FRIEND,
      ALWAYS
    )
  }

  object StudyInvite {
    val NEVER  = 1
    val FRIEND = 2
    val ALWAYS = 3

    val choices = Seq(
      NEVER,
      FRIEND,
      ALWAYS
    )
  }

  object ResizeHandle {
    val NEVER   = 0
    val INITIAL = 1
    val ALWAYS  = 2

    val choices = Seq(
      NEVER,
      INITIAL,
      ALWAYS
    )
  }

  object Zen extends BooleanPref

  def create(id: String) = default.copy(_id = id)

  lazy val default = Pref(
    _id = "",
    dark = true,
    transp = false,
    bgImg = none,
    theme = Theme.default.key,
    customTheme = none,
    pieceSet = PieceSet.default.key,
    chuPieceSet = ChuPieceSet.default.key,
    kyoPieceSet = KyoPieceSet.default.key,
    soundSet = SoundSet.default.key,
    blindfold = Blindfold.NO,
    takeback = Takeback.ALWAYS,
    moretime = Moretime.ALWAYS,
    clockSound = true,
    premove = true,
    boardLayout = BoardLayout.DEFAULT,
    animation = 2,
    coords = Coords.OUTSIDE,
    clearHands = false,
    handsBackground = false,
    follow = true,
    highlightLastDests = true,
    highlightCheck = true,
    squareOverlay = true,
    destination = true,
    dropDestination = true,
    replay = Replay.ALWAYS,
    colorName = ColorName.LANG,
    clockTenths = ClockTenths.LOWTIME,
    clockCountdown = ClockCountdown.THREE,
    challenge = Challenge.ALWAYS,
    message = Message.ALWAYS,
    studyInvite = StudyInvite.ALWAYS,
    coordColor = Color.RANDOM,
    submitMove = SubmitMove.CORRESPONDENCE_ONLY,
    confirmResign = ConfirmResign.YES,
    insightsShare = false,
    thickGrid = ThickGrid.NO,
    keyboardMove = KeyboardMove.NO,
    zen = Zen.NO,
    moveEvent = MoveEvent.BOTH,
    notation = Notations.western.index,
    resizeHandle = ResizeHandle.INITIAL,
    tags = Map.empty
  )

  import ornicar.scalalib.Zero
  implicit def PrefZero: Zero[Pref] = Zero.instance(default)
}

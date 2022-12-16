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
    follow: Boolean,
    coords: Int,
    highlightLastDests: Boolean,
    highlightCheck: Boolean,
    squareOverlay: Boolean,
    destination: Boolean,
    dropDestination: Boolean,
    replay: Int,
    challenge: Int,
    message: Int,
    studyInvite: Int,
    coordColor: Int,
    submitMove: Int,
    confirmResign: Int,
    insightShare: Int,
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

  def coordColorName = Color.choices.toMap.get(coordColor).fold("random")(_.toLowerCase)
  def coordsClass    = Coords classOf coords

  def hasSeenVerifyTitle = tags contains Tag.verifyTitle

  def set(name: String, value: String): Option[Pref] =
    name match {
      case "bg" =>
        if (value == "transp") copy(dark = true, transp = true).some
        else copy(dark = value == "dark", transp = false).some
      case "bgImg" => copy(bgImg = value.some).some
      case "theme" =>
        Theme.allByName get value map { t =>
          copy(theme = t.name)
        }
      case "pieceSet" =>
        PieceSet.allByName get value map { p =>
          copy(pieceSet = p.name)
        }
      case "chuPieceSet" =>
        ChuPieceSet.allByName get value map { p =>
          copy(chuPieceSet = p.name)
        }
      case "soundSet" =>
        SoundSet.allByKey get value map { s =>
          copy(soundSet = s.key)
        }
      case "zen" => copy(zen = if (value == "1") 1 else 0).some
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

  def isZen = zen == Zen.YES
}

object Pref {

  val defaultBgImg = "//lishogi1.org/assets/images/background/landscape.jpg"

  trait BooleanPref {
    val NO      = 0
    val YES     = 1
    val choices = Seq(NO -> "No", YES -> "Yes")
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

    val choices = Seq(
      SENTE  -> "Sente",
      RANDOM -> "Random",
      GOTE   -> "Gote"
    )
  }

  object AutoPromote {
    val NEVER   = 1
    val PREMOVE = 2
    val ALWAYS  = 3

    val choices = Seq(
      NEVER   -> "Never",
      ALWAYS  -> "Always",
      PREMOVE -> "When premoving"
    )
  }

  object SubmitMove {
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
  }

  object ConfirmResign extends BooleanPref

  object InsightShare {
    val NOBODY    = 0
    val FRIENDS   = 1
    val EVERYBODY = 2

    val choices = Seq(
      NOBODY    -> "With nobody",
      FRIENDS   -> "With friends",
      EVERYBODY -> "With everybody"
    )
  }

  object KeyboardMove extends BooleanPref

  object MoveEvent {
    val CLICK = 0
    val DRAG  = 1
    val BOTH  = 2

    val choices = Seq(
      CLICK -> "Click two squares",
      DRAG  -> "Drag a piece",
      BOTH  -> "Both clicks and drag"
    )
  }

  object Blindfold extends BooleanPref {
    override val choices = Seq(
      NO  -> "What? No!",
      YES -> "Yes, hide the pieces"
    )
  }

  object Takeback {
    val NEVER  = 1
    val CASUAL = 2
    val ALWAYS = 3

    val choices = Seq(
      NEVER  -> "Never",
      ALWAYS -> "Always",
      CASUAL -> "In casual games only"
    )
  }

  object Moretime {
    val NEVER  = 1
    val CASUAL = 2
    val ALWAYS = 3

    val choices = Seq(
      NEVER  -> "Never",
      ALWAYS -> "Always",
      CASUAL -> "In casual games only"
    )
  }

  object BoardLayout {
    val SIDE    = 0
    val COMPACT = 1

    val choices = Seq(
      SIDE    -> "Side",
      COMPACT -> "Compact"
    )
  }

  object Animation {
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
  }

  object Coords {
    val NONE    = 0
    val INSIDE  = 1
    val OUTSIDE = 2
    val EDGE    = 3

    val choices = Seq(
      NONE    -> "No",
      INSIDE  -> "Inside the board",
      OUTSIDE -> "Outside the board",
      EDGE    -> "Edge of the board"
    )

    def classOf(v: Int) =
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
      NEVER  -> "Never",
      SLOW   -> "On slow games",
      ALWAYS -> "Always"
    )
  }

  object ClockTenths {
    val NEVER   = 0
    val LOWTIME = 1
    val ALWAYS  = 2

    val choices = Seq(
      NEVER   -> "Never",
      LOWTIME -> "When time remaining < 10 seconds",
      ALWAYS  -> "Always"
    )
  }

  object ClockCountdown {
    val NEVER = 0
    val THREE = 3
    val FIVE  = 5
    val TEN   = 10

    val choices = Seq(
      NEVER -> "Never",
      THREE -> "Last three seconds",
      FIVE  -> "Last five seconds",
      TEN   -> "Last ten seconds"
    )
  }

  object Challenge {
    val NEVER  = 1
    val RATING = 2
    val FRIEND = 3
    val ALWAYS = 4

    val ratingThreshold = 300

    val choices = Seq(
      NEVER  -> "Never",
      RATING -> s"If rating is ± $ratingThreshold",
      FRIEND -> "Only friends",
      ALWAYS -> "Always"
    )
  }

  object Message {
    val NEVER  = 1
    val FRIEND = 2
    val ALWAYS = 3

    val choices = Seq(
      NEVER  -> "Never",
      FRIEND -> "Only friends",
      ALWAYS -> "Always"
    )
  }

  object StudyInvite {
    val NEVER  = 1
    val FRIEND = 2
    val ALWAYS = 3

    val choices = Seq(
      NEVER  -> "Never",
      FRIEND -> "Only friends",
      ALWAYS -> "Always"
    )
  }

  object ResizeHandle {
    val NEVER   = 0
    val INITIAL = 1
    val ALWAYS  = 2

    val choices = Seq(
      NEVER   -> "Never",
      INITIAL -> "On initial position",
      ALWAYS  -> "Always"
    )
  }

  object Zen extends BooleanPref {}

  def create(id: String) = default.copy(_id = id)

  lazy val default = Pref(
    _id = "",
    dark = false,
    transp = false,
    bgImg = none,
    theme = Theme.default.name,
    customTheme = none,
    pieceSet = PieceSet.default.name,
    chuPieceSet = ChuPieceSet.default.name,
    soundSet = SoundSet.default.name,
    blindfold = Blindfold.NO,
    takeback = Takeback.ALWAYS,
    moretime = Moretime.ALWAYS,
    clockSound = true,
    premove = true,
    boardLayout = BoardLayout.SIDE,
    animation = 2,
    follow = true,
    highlightLastDests = true,
    highlightCheck = true,
    squareOverlay = true,
    destination = true,
    dropDestination = true,
    coords = Coords.OUTSIDE,
    replay = Replay.ALWAYS,
    clockTenths = ClockTenths.LOWTIME,
    clockCountdown = ClockCountdown.THREE,
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
    notation = Notations.western.index,
    resizeHandle = ResizeHandle.INITIAL,
    tags = Map.empty
  )

  import ornicar.scalalib.Zero
  implicit def PrefZero: Zero[Pref] = Zero.instance(default)
}

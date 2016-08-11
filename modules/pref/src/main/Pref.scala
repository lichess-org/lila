package lila.pref

import lila.user.User

case class Pref(
    _id: String, // user id
    dark: Boolean,
    transp: Boolean,
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
    coordColor: Int,
    puzzleDifficulty: Int,
    submitMove: Int,
    confirmResign: Int,
    insightShare: Int,
    keyboardMove: Int,
    moveEvent: Int,
    tags: Map[String, String] = Map.empty) {

  import Pref._

  def id = _id

  def realTheme = Theme(theme)
  def realPieceSet = PieceSet(pieceSet)
  def realTheme3d = Theme3d(theme3d)
  def realPieceSet3d = PieceSet3d(pieceSet3d)

  def realSoundSet = SoundSet(soundSet)

  def coordColorName = Color.choices.toMap.get(coordColor).fold("random")(_.toLowerCase)

  def hasSeenVerifyTitle = tags contains Tag.verifyTitle

  def get(name: String): Option[String] = name match {
    case "bg"         => transp.fold("transp", dark.fold("dark", "light")).some
    case "bgImg"      => bgImg
    case "theme"      => theme.some
    case "pieceSet"   => pieceSet.some
    case "theme3d"    => theme3d.some
    case "pieceSet3d" => pieceSet3d.some
    case "is3d"       => is3d.toString.some
    case "soundSet"   => soundSet.some
    case _            => none
  }
  def set(name: String, value: String): Option[Pref] = name match {
    case "bg" =>
      if (value == "transp") copy(dark = true, transp = true).some
      else Pref.bgs get value map { b => copy(dark = b, transp = false) }
    case "bgImg"      => copy(bgImg = value.some).some
    case "theme"      => Theme.allByName get value map { t => copy(theme = t.name) }
    case "pieceSet"   => PieceSet.allByName get value map { p => copy(pieceSet = p.name) }
    case "theme3d"    => Theme3d.allByName get value map { t => copy(theme3d = t.name) }
    case "pieceSet3d" => PieceSet3d.allByName get value map { p => copy(pieceSet3d = p.name) }
    case "is3d"       => copy(is3d = value == "true").some
    case "soundSet"   => SoundSet.allByKey get value map { s => copy(soundSet = s.name) }
    case _            => none
  }

  def animationFactor = animation match {
    case Animation.NONE   => 0
    case Animation.FAST   => 0.5f
    case Animation.NORMAL => 1
    case Animation.SLOW   => 2
    case _                => 1
  }

  def isBlindfold = blindfold == Pref.Blindfold.YES

  def bgImgOrDefault = bgImg | Pref.defaultBgImg
}

object Pref {

  val defaultBgImg = "//lichess1.org/assets/images/background/landscape.jpg"

  trait BooleanPref {
    val NO = 0
    val YES = 1
    val choices = Seq(NO -> "No", YES -> "Yes")
  }

  object Tag {
    val verifyTitle = "verifyTitle"
  }

  object Difficulty {
    val EASY = 1
    val NORMAL = 2
    val HARD = 3

    val choices = Seq(
      EASY -> "Easy",
      NORMAL -> "Normal",
      HARD -> "Hard")
  }

  object Color {
    val WHITE = 1
    val RANDOM = 2
    val BLACK = 3

    val choices = Seq(
      WHITE -> "White",
      RANDOM -> "Random",
      BLACK -> "Black")
  }

  object AutoQueen {
    val NEVER = 1
    val PREMOVE = 2
    val ALWAYS = 3

    val choices = Seq(
      NEVER -> "Never",
      ALWAYS -> "Always",
      PREMOVE -> "When premoving")
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
      ALWAYS -> "Always")
  }

  object ConfirmResign extends BooleanPref

  object InsightShare {
    val NOBODY = 0
    val FRIENDS = 1
    val EVERYBODY = 2

    val choices = Seq(
      NOBODY -> "With nobody",
      FRIENDS -> "With friends",
      EVERYBODY -> "With everybody")
  }

  object KeyboardMove extends BooleanPref

  object MoveEvent {
    val CLICK = 0
    val DRAG = 1
    val BOTH = 2

    val choices = Seq(
      CLICK -> "Click two squares",
      DRAG -> "Drag a piece",
      BOTH -> "Both clicks and drag")
  }

  object Blindfold extends BooleanPref {
    override val choices = Seq(
      NO -> "What? No!",
      YES -> "Yes, hide the pieces")
  }

  object AutoThreefold {
    val NEVER = 1
    val TIME = 2
    val ALWAYS = 3

    val choices = Seq(
      NEVER -> "Never",
      ALWAYS -> "Always",
      TIME -> "When time remaining < 30 seconds")
  }

  object Takeback {
    val NEVER = 1
    val CASUAL = 2
    val ALWAYS = 3

    val choices = Seq(
      NEVER -> "Never",
      ALWAYS -> "Always",
      CASUAL -> "In casual games only")
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
      SLOW -> "Slow")
  }

  object Coords {
    val NONE = 0
    val INSIDE = 1
    val OUTSIDE = 2

    val choices = Seq(
      NONE -> "No",
      INSIDE -> "Inside the board",
      OUTSIDE -> "Outside the board")
  }

  object Replay {
    val NEVER = 0
    val SLOW = 1
    val ALWAYS = 2

    val choices = Seq(
      NEVER -> "Never",
      SLOW -> "On slow games",
      ALWAYS -> "Always")
  }

  object ClockTenths {
    val NEVER = 0
    val LOWTIME = 1
    val ALWAYS = 2

    val choices = Seq(
      NEVER -> "Never",
      LOWTIME -> "When time remaining < 10 seconds",
      ALWAYS -> "Always")
  }

  object Challenge {
    val NEVER = 1
    val RATING = 2
    val FRIEND = 3
    val ALWAYS = 4

    private val ratingThreshold = 500

    val choices = Seq(
      NEVER -> "Never",
      RATING -> s"If rating is ± $ratingThreshold",
      FRIEND -> "Only friends",
      ALWAYS -> "Always")

    def block(from: User, to: User, pref: Int, follow: Boolean, fromCheat: Boolean): Option[String] = pref match {
      case NEVER => "{{user}} doesn't accept challenges.".some
      case _ if fromCheat && !follow => "{{user}} only accepts challenges from friends.".some
      case RATING if from.perfs.bestRating > to.perfs.bestRating => none
      case RATING if math.abs(from.perfs.bestRating - to.perfs.bestRating) > ratingThreshold =>
        s"{{user}} only accepts challenges if rating is ± $ratingThreshold.".some
      case FRIEND if !follow => "{{user}} only accepts challenges from friends.".some
      case _                 => none
    }
  }

  object Message {
    val NEVER = 1
    val FRIEND = 2
    val ALWAYS = 3

    val choices = Seq(
      NEVER -> "Never",
      FRIEND -> "Only friends",
      ALWAYS -> "Always")
  }

  def create(id: String) = default.copy(_id = id)

  lazy val default = Pref(
    _id = "",
    dark = false,
    transp = false,
    bgImg = none,
    is3d = false,
    theme = Theme.default.name,
    pieceSet = PieceSet.default.name,
    theme3d = Theme3d.default.name,
    pieceSet3d = PieceSet3d.default.name,
    soundSet = SoundSet.default.name,
    blindfold = Blindfold.NO,
    autoQueen = AutoQueen.PREMOVE,
    autoThreefold = AutoThreefold.TIME,
    takeback = Takeback.ALWAYS,
    clockBar = true,
    clockSound = true,
    premove = true,
    animation = 2,
    captured = true,
    follow = true,
    highlight = true,
    destination = true,
    coords = Coords.OUTSIDE,
    replay = Replay.ALWAYS,
    clockTenths = ClockTenths.LOWTIME,
    challenge = Challenge.ALWAYS,
    message = Message.ALWAYS,
    coordColor = Color.RANDOM,
    puzzleDifficulty = Difficulty.NORMAL,
    submitMove = SubmitMove.CORRESPONDENCE_ONLY,
    confirmResign = ConfirmResign.YES,
    insightShare = InsightShare.FRIENDS,
    keyboardMove = KeyboardMove.NO,
    moveEvent = MoveEvent.BOTH,
    tags = Map.empty)

  import ornicar.scalalib.Zero
  implicit def PrefZero: Zero[Pref] = Zero.instance(default)

  private val bgs = Map("light" -> false, "dark" -> true)
}

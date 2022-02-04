package shogi
package format

import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat

case class Tag(name: TagType, value: String) {

  override def toString = s"""$name：$value"""
}

sealed trait TagType {
  lazy val name      = toString
  lazy val lowercase = name.toLowerCase
  lazy val kifName   = Tag.tagToKifName.get(this) | name
  lazy val csaName   = Tag.tagToCsaName.get(this) | name.toUpperCase
  val isUnknown      = false
}

case class Tags(value: List[Tag]) extends AnyVal {

  def apply(name: String): Option[String] = {
    val tagType = Tag tagType name
    value.find(_.name == tagType).map(_.value)
  }

  def apply(which: Tag.type => TagType): Option[String] =
    value find (_.name == which(Tag)) map (_.value)

  def clockConfig: Option[Clock.Config] = {
    val baseClk = value.collectFirst { case Tag(Tag.TimeControl, str) =>
      str
    } flatMap { c => Clock.readKifConfig(c) orElse Clock.readCsaConfig(c) }
    apply(_.Byoyomi)
      .flatMap(Clock.parseJPTime _)
      .fold(baseClk)(byo => baseClk.map(_.copy(byoyomiSeconds = byo)))
  }

  def variant: Option[shogi.variant.Variant] =
    apply(_.Variant).map(_.toLowerCase).flatMap { case name =>
      shogi.variant.Variant byName name
    }

  def anyDate: Option[String] = apply(_.UTCDate) orElse apply(_.Start) orElse apply(_.End)

  def year: Option[Int] =
    anyDate flatMap {
      case Tags.DateRegex(y, _, _) => y.toIntOption
      case _                       => None
    }

  def sfen: Option[format.forsyth.Sfen] = apply(_.Sfen) map format.forsyth.Sfen.apply

  def exists(which: Tag.type => TagType): Boolean =
    value.exists(_.name == which(Tag))

  def resultColor: Option[Option[Color]] =
    apply(_.Result).filter("*" !=) map Color.fromResult

  def knownTypes: Tags = Tags(value.filter(Tag.tagTypes contains _.name))

  def ++(tags: Tags) = tags.value.foldLeft(this)(_ + _)

  def +(tag: Tag) = Tags(value.filterNot(_.name == tag.name) :+ tag)

  override def toString = value mkString "\n"
}

object Tags {
  val empty = Tags(Nil)

  private val DateRegex = """(\d{4}|\?{4})\.(\d\d|\?\?)\.(\d\d|\?\?)""".r
}

object Tag {

  case object Event extends TagType
  case object Site  extends TagType
  case object Start extends TagType
  case object End   extends TagType
  case object UTCDate extends TagType {
    val format = DateTimeFormat forPattern "yyyy/MM/dd" withZone DateTimeZone.UTC
  }
  case object UTCTime extends TagType {
    val format = DateTimeFormat forPattern "HH:mm:ss" withZone DateTimeZone.UTC
  }
  case object Sente       extends TagType
  case object Gote        extends TagType
  case object TimeControl extends TagType
  case object SenteClock  extends TagType
  case object GoteClock   extends TagType
  case object SenteElo    extends TagType
  case object GoteElo     extends TagType
  case object SenteTitle  extends TagType
  case object GoteTitle   extends TagType
  case object SenteTeam   extends TagType
  case object GoteTeam    extends TagType
  case object Result      extends TagType
  case object Sfen        extends TagType
  case object Variant     extends TagType
  case object Opening     extends TagType
  case object Termination extends TagType
  case object Annotator   extends TagType
  case object Handicap    extends TagType
  case object Byoyomi     extends TagType
  case class Unknown(n: String) extends TagType {
    override def toString  = n
    override val isUnknown = true
  }
  // Tsume tags
  case object ProblemName       extends TagType
  case object ProblemId         extends TagType
  case object DateOfPublication extends TagType
  case object Composer          extends TagType
  case object Publication       extends TagType
  case object Collection        extends TagType
  case object Length            extends TagType
  case object Prize             extends TagType

  val tsumeTypes = List(
    ProblemName,
    ProblemId,
    DateOfPublication,
    Composer,
    Publication,
    Collection,
    Length,
    Prize
  )

  val tagTypes = List(
    Event,
    Site,
    Start,
    End,
    UTCDate,
    UTCTime,
    Sente,
    Gote,
    TimeControl,
    SenteClock,
    GoteClock,
    SenteElo,
    GoteElo,
    SenteTitle,
    GoteTitle,
    SenteTeam,
    GoteTeam,
    Result,
    Sfen,
    Variant,
    Opening,
    Termination,
    Annotator,
    Handicap,
    Byoyomi
  ) ++ tsumeTypes

  val tagTypesByLowercase: Map[String, TagType] =
    tagTypes
      .map { t =>
        t.lowercase -> t
      }
      .toMap

  def apply(name: String, value: Any): Tag =
    new Tag(
      name = tagType(name),
      value = value.toString
    )

  def apply(name: Tag.type => TagType, value: Any): Tag =
    new Tag(
      name = name(this),
      value = value.toString
    )

  def tagType(name: String) =
    (tagTypesByLowercase get (kifNameToTag.get(name).fold(name.toLowerCase)(_.lowercase))) | Unknown(name)

  val tagToCsaName = Map[TagType, String](
    Tag.Start       -> "START_TIME",
    Tag.End         -> "END_TIME",
    Tag.TimeControl -> "TIME_LIMIT",
    Tag.Sente       -> "+",
    Tag.Gote        -> "-"
  )

  val csaNameToTag =
    (tagToCsaName map { case (k, v) => v -> k }) ++ Map(
      "Name+" -> Tag.Sente,
      "Name-" -> Tag.Gote
    )

  val tagToKifName = Map[TagType, String](
    Tag.Start             -> "開始日時",
    Tag.End               -> "終了日時",
    Tag.Event             -> "棋戦",
    Tag.Site              -> "場所",
    Tag.Opening           -> "戦型",
    Tag.TimeControl       -> "持ち時間",
    Tag.Byoyomi           -> "秒読み",
    Tag.Handicap          -> "手合割",
    Tag.Sente             -> "先手",
    Tag.Gote              -> "後手",
    Tag.SenteTeam         -> "先手のチーム",
    Tag.GoteTeam          -> "後手のチーム",
    Tag.Annotator         -> "注釈者",
    Tag.Termination       -> "図",
    Tag.ProblemName       -> "作品名",
    Tag.ProblemId         -> "作品番号",
    Tag.Composer          -> "作者",
    Tag.DateOfPublication -> "発表年月",
    Tag.Publication       -> "発表誌",
    Tag.Collection        -> "出典",
    Tag.Length            -> "手数",
    Tag.Prize             -> "受賞"
  )

  val kifNameToTag =
    (tagToKifName map { case (k, v) => v -> k }) ++ Map(
      "下手"  -> Tag.Sente,
      "上手"  -> Tag.Gote,
      "対局日" -> Tag.Start
    )

  def timeControlKif(clock: Option[Clock.Config]) =
    Tag(
      TimeControl,
      clock.fold("") { c =>
        val init =
          if (c.limit.roundSeconds % 60 == 0) s"${c.limit.roundSeconds / 60}分"
          else s"${c.limit.roundSeconds}秒"
        val byo =
          if (c.hasByoyomi) s"+${c.byoyomi.roundSeconds}秒"
          else ""
        val periods =
          if (c.periodsTotal > 1) s"(${c.periodsTotal})"
          else ""
        val inc =
          if (c.hasIncrement) s"+${c.increment.roundSeconds}秒"
          else ""
        s"$init$byo$periods$inc"
      }
    )

  def timeControlCsa(clock: Option[Clock.Config]) =
    Tag(
      TimeControl,
      clock.fold("") { c =>
        val init =
          if (c.limit.roundSeconds % 60 == 0) f"00:${c.limit.roundSeconds / 60}%02d"
          else f"00:00:${c.limit.roundSeconds}%02d"
        val byo =
          if (c.hasByoyomi) f"+${c.byoyomi.roundSeconds}%02d"
          else ""
        s"$init$byo"
      }
    )
}

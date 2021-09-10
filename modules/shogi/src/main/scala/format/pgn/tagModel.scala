package shogi
package format.pgn

import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat

case class Tag(name: TagType, value: String) {

  override def toString = s"""$name：$value"""
}

sealed trait TagType {
  lazy val name      = toString
  lazy val lowercase = name.toLowerCase
  lazy val kifName   = KifUtils.tagToKif.get(this).getOrElse(name)
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
    } flatMap Clock.readKifConfig
    apply(_.Byoyomi).flatMap(Clock.parseJPTime _).fold(baseClk)(byo => baseClk.map(_.copy(byoyomiSeconds = byo)))
  }

  def variant: Option[shogi.variant.Variant] =
    apply(_.Variant).map(_.toLowerCase).flatMap { case name =>
      shogi.variant.Variant byName name
    }

  def anyDate: Option[String] = apply(_.UTCDate) orElse apply(_.Start) orElse apply(_.End)

  def year: Option[Int] =
    anyDate flatMap {
      case Tags.DateRegex(y, _, _) => parseIntOption(y)
      case _                       => None
    }

  def fen: Option[format.FEN] = apply(_.FEN) map format.FEN.apply

  def exists(which: Tag.type => TagType): Boolean =
    value.exists(_.name == which(Tag))

  def resultColor: Option[Option[Color]] =
    apply(_.Result).filter("*" !=) map Color.fromResult

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
  case object Sente           extends TagType
  case object Gote            extends TagType
  case object TimeControl     extends TagType
  case object SenteClock      extends TagType
  case object GoteClock       extends TagType
  case object SenteElo        extends TagType
  case object GoteElo         extends TagType
  case object SenteTitle      extends TagType
  case object GoteTitle       extends TagType
  case object SenteTeam       extends TagType
  case object GoteTeam        extends TagType
  case object Result          extends TagType
  case object FEN             extends TagType
  case object Variant         extends TagType
  case object Opening         extends TagType
  case object Termination     extends TagType
  case object Annotator       extends TagType
  case object Handicap        extends TagType
  case object Byoyomi         extends TagType
  case class Unknown(n: String) extends TagType {
    override def toString  = n
    override val isUnknown = true
  }
  // Tsume tags
  case object ProblemName         extends TagType
  case object ProblemId           extends TagType
  case object DateOfPublication   extends TagType
  case object Composer            extends TagType
  case object Publication         extends TagType
  case object Collection          extends TagType
  case object Length              extends TagType
  case object Prize               extends TagType

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
    FEN,
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
      .to(Map)

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
    (tagTypesByLowercase get KifUtils.normalizeKifName(name).toLowerCase) | Unknown(name)

  def timeControl(clock: Option[Clock.Config]) =
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
          if (c.periods > 1) s"(${c.periods})"
          else ""
        val inc =
          if(c.hasIncrement) s"+${c.increment.roundSeconds}秒"
          else ""
        s"$init$byo$periods$inc"
      }
    )
}

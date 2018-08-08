package draughts
package format.pdn

import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat

case class Tag(name: TagType, value: String) {

  override def toString = s"""[$name "$value"]"""
}

sealed trait TagType {
  lazy val name = toString
  lazy val lowercase = name.toLowerCase
  val isUnknown = false
}

case class Tags(value: List[Tag]) extends AnyVal {

  def apply(name: String): Option[String] = {
    val tagType = Tag tagType name
    value.find(_.name == tagType).map(_.value)
  }

  def apply(which: Tag.type => TagType): Option[String] =
    value find (_.name == which(Tag)) map (_.value)

  def clockConfig: Option[Clock.Config] =
    value.collectFirst {
      case Tag(Tag.TimeControl, str) => str
    } flatMap Clock.readPdnConfig

  def variant: Option[draughts.variant.Variant] =
    apply(_.GameType).fold(apply(_.Variant) flatMap draughts.variant.Variant.byName) {
      case Tags.GameTypeRegex(t, _*) => parseIntOption(t) match {
        case Some(gameType) => draughts.variant.Variant byGameType gameType
        case _ => None
      }
      case _ => None
    }

  def anyDate: Option[String] = apply(_.UTCDate) orElse apply(_.Date)

  def year: Option[Int] = anyDate flatMap {
    case Tags.DateRegex(y, _, _) => parseIntOption(y)
    case _ => None
  }

  def fen: Option[format.FEN] = apply(_.FEN) map format.FEN.apply

  def exists(which: Tag.type => TagType): Boolean =
    value.exists(_.name == which(Tag))

  def resultColor: Option[Option[Color]] =
    apply(_.Result).filter("*" !=) map Color.fromResult

  def ++(tags: Tags) = tags.value.foldLeft(this)(_ + _)

  def +(tag: Tag) = Tags(value.filterNot(_.name == tag.name) :+ tag)
}

object Tags {
  val empty = Tags(Nil)

  private val DateRegex = """(\d{4}|\?{4})\.(\d\d|\?\?)\.(\d\d|\?\?)""".r
  private val GameTypeRegex = """([0-9]+)(,[WB],[0-9]+,[0-9]+,[ANS][0123](,[01])?)?""".r
}

object Tag {

  case object Event extends TagType
  case object Site extends TagType
  case object Date extends TagType
  case object UTCDate extends TagType {
    val format = DateTimeFormat forPattern "yyyy.MM.dd" withZone DateTimeZone.UTC
  }
  case object UTCTime extends TagType {
    val format = DateTimeFormat forPattern "HH:mm:ss" withZone DateTimeZone.UTC
  }
  case object Round extends TagType
  case object White extends TagType
  case object Black extends TagType
  case object TimeControl extends TagType
  case object WhiteClock extends TagType
  case object BlackClock extends TagType
  case object WhiteElo extends TagType
  case object BlackElo extends TagType
  case object WhiteRating extends TagType
  case object BlackRating extends TagType
  case object WhiteRatingDiff extends TagType
  case object BlackRatingDiff extends TagType
  case object WhiteTitle extends TagType
  case object BlackTitle extends TagType
  case object WhiteTeam extends TagType
  case object BlackTeam extends TagType
  case object Result extends TagType
  case object FEN extends TagType
  case object Variant extends TagType
  case object GameType extends TagType
  case object ECO extends TagType
  case object Opening extends TagType
  case object Termination extends TagType
  case object Annotator extends TagType
  case class Unknown(n: String) extends TagType {
    override def toString = n
    override val isUnknown = true
  }

  val tagTypes = List(
    Event, Site, Date, UTCDate, UTCTime, Round, White, Black, TimeControl,
    WhiteClock, BlackClock, WhiteElo, BlackElo, WhiteRating, BlackRating, WhiteRatingDiff, BlackRatingDiff, WhiteTitle, BlackTitle,
    WhiteTeam, BlackTeam, Result, FEN, Variant, GameType, ECO, Opening, Termination, Annotator
  )
  val tagTypesByLowercase: Map[String, TagType] =
    tagTypes.map { t => t.lowercase -> t }(scala.collection.breakOut)

  def apply(name: String, value: Any): Tag = new Tag(
    name = tagType(name),
    value = value.toString
  )

  def apply(name: Tag.type => TagType, value: Any): Tag = new Tag(
    name = name(this),
    value = value.toString
  )

  def tagType(name: String) =
    (tagTypesByLowercase get name.toLowerCase) | Unknown(name)

  def timeControl(clock: Option[Clock.Config]) =
    Tag(TimeControl, clock.fold("-") { c =>
      s"${c.limit.roundSeconds}+${c.increment.roundSeconds}"
    })
}

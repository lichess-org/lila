package lila.study

import chess.format.pgn.{ Tag, Tags, TagType }

object PgnTags {

  def apply(tags: Tags): Tags = Tags(sort(tags.value filter isRelevant))

  def setRootClockFromTags(c: Chapter): Option[Chapter] =
    c.updateRoot { _.setClockAt(c.tags.clockConfig map (_.limit), Path.root) } filter (c !=)

  private def isRelevant(tag: Tag) =
    relevantTypeSet(tag.name) && !unknownValues(tag.value)

  private val unknownValues = Set("", "?", "unknown")

  private val sortedTypes: List[TagType] = {
    import Tag._
    List(
      White, WhiteElo, WhiteTitle, WhiteTeam,
      Black, BlackElo, BlackTitle, BlackTeam,
      TimeControl,
      Date,
      Result,
      Termination,
      Site, Event, Round, Annotator
    )
  }

  val typesToString = sortedTypes mkString ","

  private val relevantTypeSet: Set[TagType] = sortedTypes.toSet

  private val typePositions: Map[TagType, Int] = sortedTypes.zipWithIndex.toMap

  private def sort(tags: List[Tag]) =
    tags.sortBy { t =>
      typePositions.getOrElse(t.name, Int.MaxValue)
    }
}

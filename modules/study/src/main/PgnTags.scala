package lila.study

import scala.util.chaining._

import chess.format.pgn.{ Tag, TagType, Tags }

object PgnTags {

  def apply(tags: Tags): Tags =
    tags pipe filterRelevant pipe removeContradictingTermination pipe sort

  def setRootClockFromTags(c: Chapter): Option[Chapter] =
    c.updateRoot { _.setClockAt(c.tags.clockConfig map (_.limit), Path.root) } filter (c !=)

  private def filterRelevant(tags: Tags) =
    Tags(tags.value.filter { t =>
      relevantTypeSet(t.name) && !unknownValues(t.value)
    })

  private def removeContradictingTermination(tags: Tags) =
    if (tags.resultColor.isDefined)
      Tags(tags.value.filterNot { t =>
        t.name == Tag.Termination && t.value.toLowerCase == "unterminated"
      })
    else tags

  private val unknownValues = Set("", "?", "unknown")

  private val sortedTypes: List[TagType] = {
    import Tag._
    List(
      White,
      WhiteElo,
      WhiteTitle,
      WhiteTeam,
      Black,
      BlackElo,
      BlackTitle,
      BlackTeam,
      TimeControl,
      Date,
      Result,
      Termination,
      Site,
      Event,
      Round,
      Board,
      Annotator
    )
  }

  val typesToString = sortedTypes mkString ","

  private val relevantTypeSet: Set[TagType] = sortedTypes.toSet

  private val typePositions: Map[TagType, Int] = sortedTypes.zipWithIndex.toMap

  private def sort(tags: Tags) =
    Tags {
      tags.value.sortBy { t =>
        typePositions.getOrElse(t.name, Int.MaxValue)
      }
    }
}

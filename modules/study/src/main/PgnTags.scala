package lila.study

import scala.util.chaining.*

import chess.format.pgn.{ Tag, TagType, Tags }
import chess.format.UciPath

object PgnTags:

  def apply(tags: Tags): Tags =
    tags pipe filterRelevant pipe removeContradictingTermination pipe sort

  def setRootClockFromTags(c: Chapter): Option[Chapter] =
    c.updateRoot { _.setClockAt(c.tags.clockConfig map (_.limit), UciPath.root) } filter (c !=)

  private def filterRelevant(tags: Tags) =
    Tags(tags.value.filter { t =>
      relevantTypeSet(t.name) && !unknownValues(t.value)
    })

  private def removeContradictingTermination(tags: Tags) =
    if tags.outcome.isDefined then
      Tags(tags.value.filterNot { t =>
        t.name == Tag.Termination && t.value.toLowerCase == "unterminated"
      })
    else tags

  private val unknownValues = Set("", "?", "unknown")

  private val sortedTypes: List[TagType] =
    import Tag.*
    List(
      White,
      WhiteElo,
      WhiteTitle,
      WhiteTeam,
      WhiteFideId,
      Black,
      BlackElo,
      BlackTitle,
      BlackTeam,
      BlackFideId,
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

  val typesToString = sortedTypes mkString ","

  private val relevantTypeSet: Set[TagType] = sortedTypes.toSet

  private val typePositions: Map[TagType, Int] = sortedTypes.zipWithIndex.toMap

  private def sort(tags: Tags) =
    Tags {
      tags.value.sortBy { t =>
        typePositions.getOrElse(t.name, Int.MaxValue)
      }
    }

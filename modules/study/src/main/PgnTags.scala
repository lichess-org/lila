package lila.study

import chess.format.UciPath
import chess.format.pgn.{ Tag, TagType, Tags }
import lila.tree.Clock

object PgnTags:

  def apply(tags: Tags): Tags =
    tags.pipe(filterRelevant(Set.empty)).pipe(removeContradictingTermination).pipe(sort)

  def withRelevantTags(tags: Tags, types: Set[TagType]): Tags =
    tags.pipe(filterRelevant(types)).pipe(removeContradictingTermination).pipe(sort)

  def setRootClockFromTags(c: Chapter): Option[Chapter] =
    val centis = c.tags.timeControl.map: c =>
      c.limit + c.increment
    val clock = centis.map(Clock(_, true.some))
    c.updateRoot:
      _.setClockAt(clock, UciPath.root)
    .filter(c !=)

  // clean up tags before exposing them
  def cleanUpForPublication(tags: Tags) = tags.copy(
    value = tags.value.filter:
      // we need fideId=0 to know that the player really doesn't have one,
      // and that we shouldn't alert about it or try to fix it.
      // But we don't want to publish it.
      case Tag(Tag.WhiteFideId | Tag.BlackFideId, "0") => false
      case _                                           => true
  )

  private def filterRelevant(extraTypes: Set[TagType])(tags: Tags) =
    Tags(tags.value.filter { t =>
      (relevantTypeSet(t.name) || extraTypes(t.name)) && !unknownValues(t.value)
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
      Annotator,
      FEN,
      GameId
    )

  val typesToString = sortedTypes.mkString(",")

  private val relevantTypeSet: Set[TagType] = sortedTypes.toSet

  private val typePositions: Map[TagType, Int] = sortedTypes.zipWithIndex.toMap

  private def sort(tags: Tags) =
    Tags:
      tags.value.sortBy: t =>
        typePositions.getOrElse(t.name, Int.MaxValue)

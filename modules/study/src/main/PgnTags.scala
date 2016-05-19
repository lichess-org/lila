package lila.study

import chess.format.pgn.{ Tag, TagType }

private object PgnTags {

  def apply(tags: List[Tag]) = sort(tags filter isRelevant)

  private def isRelevant(tag: Tag) = tag match {
    case Tag(t, _) if !relevantTags(t) => false
    case Tag(_, v) if unknownValues(v) => false
    case Tag(Tag.Variant, v) if v.toLowerCase == "standard" => false
    case _ => true
  }

  private val unknownValues = Set("", "?", "unknown")

  private val relevantTags: Set[TagType] = {
    import Tag._
    Set(Event, Site, Date, Round, White, Black, TimeControl,
      WhiteElo, BlackElo, WhiteTitle, BlackTitle,
      Tag.Result, Tag.FEN, Variant, Termination, Annotator)
  }

  private def sort(tags: List[Tag]) = tags.sortBy {
    case Tag(Tag.White, _) => 1
    case Tag(Tag.Black, _) => 2
    case Tag(Tag.Variant, _) => 3
    case Tag(Tag.Date, _) => 4
    case Tag(Tag.Termination, _) => 5
    case Tag(Tag.Site | Tag.Event | Tag.Round | Tag.Annotator, _) => 10
    case _ => 9
  }
}

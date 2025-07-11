package lila.puzzle

import lila.db.dsl.*
import lila.core.userId.UserId

final class PuzzleWoodpecker(colls: PuzzleColls)(using Executor):

  def puzzleList(userId: UserId): Fu[List[Puzzle]] =
    colls.round.aggregateList(50) { framework =>
      import framework.*
      Match($doc(
        PuzzleRound.BSONFields.user -> userId,
        PuzzleRound.BSONFields.win -> true  // Only won puzzles
      )) -> List(
        Sort(Ascending(PuzzleRound.BSONFields.date)),  // Sort by date for consistency
        Limit(50),
        Lookup.simple(
          from = colls.puzzle,
          as = "puzzle",
          local = PuzzleRound.BSONFields.puzzle,
          foreign = "_id"
        ),
        Unwind("puzzle"),
        ReplaceRoot($doc("$newRoot" -> "$puzzle"))
      )
    }

  def complete(userId: UserId, puzzleId: PuzzleId): Funit = funit
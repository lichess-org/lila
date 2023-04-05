package lila.tournament
import org.specs2.mutable.*

class SchedulerTest extends Specification:
  "scheduler" should:
    "all" in:
      val rightNow = instantOf(2023, 4, 5, 9, 0)
      val plans    = TournamentScheduler.allWithConflicts(rightNow.date, rightNow)
      plans === List()

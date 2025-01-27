package lila.tournament

import scala.collection.mutable
import scala.collection.SortedSet

import lila.core.i18n.Translate
import lila.tournament.Schedule.Plan

object PlanBuilder:
  /** Max window for daily or better schedules to be considered overlapping (i.e. if one starts within X hrs
    * of the other ending). Using 11.5 hrs here ensures that at least one daily is always cancelled for the
    * more important event. But, if a higher importance tourney is scheduled nearly opposite of the daily
    * (i.e. 11:00 for a monthly and 00:00 for its daily), two dailies will be cancelled... so don't do this!
    */
  private[tournament] val SCHEDULE_DAILY_OVERLAP_MINS = 690 // 11.5 hours

  // Stagger up to 40s.  This keeps the calendar clean (by being less than a minute) and is sufficient
  // to space things out.  It's a bit arbitrary, but ensures that most tournaments start close to when
  // the calendar says they start.
  private[tournament] val MAX_STAGGER_MS = 40_000L

  private[tournament] abstract class ScheduleWithInterval:
    def schedule: Schedule
    def duration: java.time.Duration
    def startsAt: Instant

    val endsAt = startsAt.plus(duration)

    def interval = TimeInterval(startsAt, endsAt)

    def overlaps(other: ScheduleWithInterval) = interval.overlaps(other.interval)

    // Note: must be kept in sync with [[SchedulerTestHelpers.ExperimentalPruner.pruneConflictsFailOnUsurp]]
    // In particular, pruneConflictsFailOnUsurp filters tourneys that couldn't possibly conflict based
    // on their hours -- so the same logic (overlaps and daily windows) must be used here.
    def conflictsWith(si2: ScheduleWithInterval) =
      val s1 = schedule
      val s2 = si2.schedule
      s1.variant == s2.variant && (
        // prevent daily && weekly within X hours of each other
        if s1.freq.isDailyOrBetter && s2.freq.isDailyOrBetter && s1.sameSpeed(s2) then
          si2.startsAt.minusMinutes(SCHEDULE_DAILY_OVERLAP_MINS).isBefore(endsAt) &&
          startsAt.minusMinutes(SCHEDULE_DAILY_OVERLAP_MINS).isBefore(si2.endsAt)
        else
          (
            s1.variant.exotic ||                         // overlapping exotic variant
              s1.hasMaxRating ||                         // overlapping same rating limit
              Schedule.Speed.similar(s1.speed, s2.speed) // overlapping similar
          ) && s1.conditions.similar(s2.conditions) && overlaps(si2)
      )

    /** Kept in sync with [[conflictsWithFailOnUsurp]].
      */
    def conflictsWith(scheds: Iterable[ScheduleWithInterval]): Boolean =
      scheds.exists(conflictsWith)

    /** Kept in sync with [[conflictsWith]].
      *
      * Raises an exception if a tourney is incorrectly usurped.
      */
    @throws[IllegalStateException]("if a tourney is incorrectly usurped")
    def conflictsWithFailOnUsurp(scheds: Iterable[ScheduleWithInterval]) =
      val conflicts   = scheds.filter(conflictsWith).toSeq
      val okConflicts = conflicts.filter(_.schedule.freq >= schedule.freq)
      if conflicts.nonEmpty && okConflicts.isEmpty then
        throw new IllegalStateException(s"Schedule [$schedule] usurped by ${conflicts}")
      conflicts.nonEmpty

  private final case class ConcreteSchedule(
      schedule: Schedule,
      startsAt: Instant,
      duration: java.time.Duration
  ) extends ScheduleWithInterval

  private def rebuildSchedule(t: Tournament) =
    t.schedule.map: s =>
      Schedule(s.freq, Schedule.Speed.fromClock(t.clock), t.variant, t.position, s.at, t.conditions)

  private[tournament] def getNewTourneys(
      existingTourneys: Iterable[Tournament],
      plans: Iterable[Plan]
  )(using Translate): List[Tournament] =
    // Prune plans using the unstaggered scheduled start time.
    val existingWithScheduledStart = existingTourneys.flatMap: t =>
      // Ignore tournaments with schedule=None - they never conflict.
      rebuildSchedule(t).map { s => ConcreteSchedule(s, s.atInstant, t.duration) }

    val prunedPlans = pruneConflicts(existingWithScheduledStart, plans)

    plansWithStagger(
      // Determine new staggers based on actual (staggered) start time of existing tourneys.
      // Unlike pruning, stagger considers Tourneys with schedule=None.
      existingTourneys.map(_.startsAt),
      prunedPlans
    ).map(_.build) // Build Tournament objects from plans

  /** Given a list of existing schedules and a list of possible new plans, returns a subset of the possible
    * plans that do not conflict with either the existing schedules or with themselves. Intended to produce
    * identical output to [[SchedulerTestHelpers.ExperimentalPruner.pruneConflictsFailOnUsurp]], but this
    * variant is more readable and has lower potential for bugs.
    */
  private[tournament] def pruneConflicts[A <: ScheduleWithInterval](
      existingSchedules: Iterable[ScheduleWithInterval],
      possibleNewPlans: Iterable[A]
  ): List[A] =
    var allPlannedSchedules = existingSchedules.toList
    possibleNewPlans
      .foldLeft(Nil): (newPlans, p) =>
        if p.conflictsWith(allPlannedSchedules) then newPlans
        else
          allPlannedSchedules ::= p
          p :: newPlans
      .reverse

  /** Given existing tourneys and possible new plans, returns new Plan objects that are staggered to avoid
    * starting at the exact same time as other plans or tourneys. Does NOT filter for conflicts.
    */
  private[tournament] def plansWithStagger(
      existingEvents: Iterable[Instant],
      plans: Iterable[Plan]
  ): List[Plan] =
    val allInstants = mutable.TreeSet.from(existingEvents)

    plans
      .foldLeft(Nil): (allAdjustedPlans, plan) =>
        val adjustedPlan = staggerPlan(plan, allInstants, MAX_STAGGER_MS)
        allInstants += adjustedPlan.startsAt
        adjustedPlan :: allAdjustedPlans
      .reverse

  /** Given a plan, return an adjusted Plan with a start time that minimizes conflicts with existing events.
    */
  private def staggerPlan(plan: Plan, existingEvents: SortedSet[Instant], maxStaggerMs: Long): Plan =
    import scala.math.Ordering.Implicits.infixOrderingOps // For comparing Instants.

    val originalStart   = plan.startsAt
    val originalStartMs = originalStart.toEpochMilli
    val maxConflictAt   = originalStart.plusMillis(maxStaggerMs)

    // Find all events that start at a similar time to the plan.
    val offsetsWithSimilarStart = existingEvents
      .iteratorFrom(originalStart)
      .takeWhile(_ <= maxConflictAt)
      .map(_.toEpochMilli - originalStartMs)
      .toSeq

    val staggerMs = findMinimalGoodSlot(0L, maxStaggerMs, offsetsWithSimilarStart)
    plan.copy(startsAt = originalStart.plusMillis(staggerMs))

  /** This method is used find a good stagger value for a new tournament. We want stagger as low as possible,
    * because that means tourneys start sooner, but we also want tournaments to be spaced out to avoid server
    * DDOS.
    *
    * The method uses Longs for convenience based on usage, but it could easily be specialized to use floating
    * points or other representations.
    *
    * Overflows are *not* checked, because although this method uses Longs, its arguments are expected to be
    * small (e.g. smaller than [[Short.MaxValue]]), and so internal math is not expected to come anywhere near
    * a Long overflow.
    *
    * @param hi
    *   must be >= low
    * @param sortedExisting
    *   must be sorted and contain only values in [low, hi]
    *
    * @return
    *   the lowest value in [low, hi] with maximal distance to elts of sortedExisting
    */
  private[tournament] def findMinimalGoodSlot(low: Long, hi: Long, sortedExisting: Iterable[Long]): Long =
    if sortedExisting.isEmpty then low
    else
      val iter    = sortedExisting.iterator
      var prevElt = iter.next
      // nothing is at low element so gap is equiv to 2x size, centered at low
      var maxGapLow = low - (prevElt - low)
      var maxGapLen = (prevElt - low) * 2L
      while iter.hasNext do
        val elt = iter.next
        if elt - prevElt > maxGapLen then
          maxGapLow = prevElt
          maxGapLen = elt - prevElt
        prevElt = elt
      // Use hi only if it's strictly better than all other gaps.
      if (hi - prevElt) * 2L > maxGapLen then hi
      else maxGapLow + maxGapLen / 2L

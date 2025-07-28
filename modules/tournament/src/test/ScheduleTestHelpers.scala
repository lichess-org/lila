package lila.tournament

import PlanBuilder.{ ScheduleWithInterval, SCHEDULE_DAILY_OVERLAP_MINS }

object ScheduleTestHelpers:
  def planSortKey(p: Schedule.Plan) =
    val s = p.schedule
    (s.variant.id.value, s.conditions.nonEmpty, if s.variant.standard then s.speed.id else 0, s.at)

  def allSchedulesAt(date: LocalDateTime) =
    TournamentScheduler.allWithConflicts(date).sortBy(planSortKey).map(_.schedule)

  val usEastZone = java.time.ZoneId.of("America/New_York")
  val parisZone = java.time.ZoneId.of("Europe/Paris")
  val datePrinter = java.time.format.DateTimeFormatter.ofPattern("MM-dd'T'HH:mm z")

  def fullDaySchedule(date: LocalDateTime) =
    val startOfDay = date.withTimeAtStartOfDay
    val dayInterval = TimeInterval(startOfDay.instant, java.time.Duration.ofDays(1))

    // Prune conflicts in a similar manner to how it is done in production i.e. TournamentScheduler:
    // schedule earlier hours first, and then add later hour schedules if they don't conflict.
    // In production, existing tournaments come from db, but the effect is the same.
    ExperimentalPruner
      .pruneConflictsFailOnUsurp(
        List.empty,
        (-24 to 23).flatMap { hours =>
          TournamentScheduler
            .allWithConflicts(startOfDay.plusHours(hours))
            .filter(_.interval.overlaps(dayInterval))
        }
      )
      .sortBy(planSortKey)
      .map(p =>
        val s = p.schedule
        val realStart = s.atInstant
        val usEastStart = realStart.atZone(usEastZone).format(datePrinter)
        val parisStart = realStart.atZone(parisZone).format(datePrinter)
        s"${s} ($usEastStart, $parisStart) ${p.minutes}m"
      )

  object ExperimentalPruner:
    import java.time.temporal.ChronoUnit
    import scala.collection.mutable.{ ArrayBuffer, LongMap }

    private val MS_PER_HR = 3600L * 1000
    // Round up to nearest whole hour and convert to ms
    private val DAILY_OVERLAP_MS = Math.ceil(SCHEDULE_DAILY_OVERLAP_MINS / 60.0).toLong * MS_PER_HR

    private def firstHourMs(s: ScheduleWithInterval) =
      s.startsAt.truncatedTo(ChronoUnit.HOURS).toEpochMilli

    /** Returns a Seq of each hour (as epoch ms) that a tournament overlaps with, for use in a hash map.
      */
    def getAllHours(s: ScheduleWithInterval) =
      (firstHourMs(s) until s.endsAt.toEpochMilli by MS_PER_HR)

    /** Returns a Seq of possible hours (as epoch ms) that another tournament could exist that would be
      * considered a conflict. This results in pulling all tournaments within a sliding window. The window is
      * smaller when the tournament is an hourly, as these only conflict with tournaments that actually
      * overlap. Daily or better tournaments can conflict with another daily or better in a larger window as
      * well as with hourlies.
      */
    def getConflictingHours(s: ScheduleWithInterval) =
      // Tourneys of daily or better can conflict with another tourney within a sliding window
      if s.schedule.freq.isDailyOrBetter then
        (firstHourMs(s) - DAILY_OVERLAP_MS until
          s.endsAt.toEpochMilli + DAILY_OVERLAP_MS by
          MS_PER_HR)
      else getAllHours(s)

    /** Given a list of existing schedules and a list of possible new plans, returns a subset of the possible
      * plans that do not conflict with either the existing schedules or with themselves.
      *
      * Returns the same result as [[Schedule.pruneConflicts]], but is asymptotically more efficient,
      * performing O(n) operations of [[ScheduleWithInterval.conflictsWith(ScheduleWithInterval):*]] rather
      * than O(n^2), n being the number of inputs.
      */
    @throws[IllegalStateException]("if a tourney is incorrectly usurped")
    def pruneConflictsFailOnUsurp[A <: ScheduleWithInterval](
        existingSchedules: Iterable[ScheduleWithInterval],
        possibleNewPlans: Iterable[A]
    ): List[A] =
      // Bucket schedules by hour for faster conflict detection
      val hourMap = LongMap.empty[ArrayBuffer[ScheduleWithInterval]]
      def addToMap(hour: Long, s: ScheduleWithInterval) =
        hourMap.getOrElseUpdate(hour, ArrayBuffer.empty).addOne(s)

      existingSchedules.foreach { s => getAllHours(s).foreach { addToMap(_, s) } }

      possibleNewPlans
        .foldLeft(Nil): (newPlans, p) =>
          val potentialConflicts = getConflictingHours(p).flatMap { hourMap.getOrElse(_, Nil) }
          if p.conflictsWithFailOnUsurp(potentialConflicts) then newPlans
          else
            getAllHours(p).foreach { addToMap(_, p) }
            p :: newPlans
        .reverse

package lila.relay

case class RelayCard(
    tour: RelayTour,
    display: RelayRound, // which round to show on the tour link
    link: RelayRound, // which round to actually link to
    crowd: Crowd, // the group crowd sum
    group: Option[RelayGroup.Name],
    alts: List[RelayRound.WithTour] // other notable tours of the group
) extends RelayRound.AndTourAndGroup:

  def errors: List[String] =
    val round = display
    round.sync.log.lastErrors.some
      .filter(_.sizeIs >= 5)
      .orElse:
        (round.hasStarted && round.sync.hasUpstream && !round.sync.ongoing)
          .option(List("Not syncing!"))
      .orElse:
        round.shouldHaveStarted1Hour.option:
          List(if round.sync.hasUpstream then "Upstream has not started" else "Nothing pushed yet")
      .orElse:
        round.looksStalled.option:
          List("No updates in a while")
      .orZero

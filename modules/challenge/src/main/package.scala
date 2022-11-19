package lila.challenge

import org.joda.time.DateTime

export lila.Lila.{ *, given }

private def inTwoWeeks = DateTime.now plusWeeks 2

private val logger = lila log "challenge"

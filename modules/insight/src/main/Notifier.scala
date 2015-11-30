package lila.insight

import lila.hub.actorApi.message.LichessThread
import lila.user.User

import akka.actor.ActorSelection

private final class Notifier(sender: String, messenger: ActorSelection) {

  def dataIsReady(u: User) {
    messenger ! LichessThread(
      from = sender,
      to = u.id,
      subject = s"""Your chess insights data is ready!""",
      message = s"""Lichess servers are done crunching data from your ${u.count.rated} rated games.

Enjoy exploring your chess stats on ${insightUrl(u)}, we hope you get valuable insights!

Cheers,

The lichess.org team""")
  }

  private def insightUrl(u: User) =
    s"http://lichess.org/insights/${u.username}"
}

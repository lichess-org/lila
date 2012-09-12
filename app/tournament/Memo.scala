package lila
package tournament

import akka.actor._
import akka.dispatch.Await
import akka.pattern.ask
import akka.util.duration._
import akka.util.Timeout
import scalaz.effects._

import memo.MonoMemo

final class Memo(hubMaster: ActorRef, ttl: Int) {

  private val atMost = 5 seconds
  private implicit val timeout = Timeout(atMost)

  private val memo = new MonoMemo(ttl, io {
    Await.result((hubMaster ? GetTournamentIds).mapTo[Int], atMost)
  })

  def tournamentIds = memo.apply

  def isTournamentInProgress(id: String) = tournamentIds contains id
}

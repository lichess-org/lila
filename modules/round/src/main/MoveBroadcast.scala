// package lila.round

// import akka.actor._

// import com.roundeights.hasher.Implicits._

// import lila.hub.actorApi.round.MoveEvent
// import play.api.libs.iteratee._

// private final class MoveBroadcast(salt: String) extends Actor {

//   context.system.lilaBus.subscribe(self, 'moveEvent)

//   override def postStop() {
//     context.system.lilaBus.unsubscribe(self)
//   }

//   private val (enumerator, channel) = Concurrent.broadcast[String]

//   def receive = {

//     case MoveBroadcast.GetEnumerator => sender ! enumerator

//     case move: MoveEvent             => channel push s"${tokenOf(move.gameId)} ${move.ip}"
//   }

//   def tokenOf(gameId: String) = gameId.salt(salt).md5.hex take 8
// }

// object MoveBroadcast {

//   case object GetEnumerator
// }

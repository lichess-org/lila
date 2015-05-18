package lila.tournament

import org.specs2.mutable.Specification
import scalaz.NonEmptyList

import org.joda.time.DateTime

class ArenaPairingPerfTest extends Specification {

  val nb = 5
  val nbPlayers = 14
  val iterations = 5
  val nbPrePairings = 10

  def makeUser(username: String) = lila.user.User(
    id = username.toLowerCase,
    username = username,
    perfs = lila.user.Perfs.default.copy(
      bullet = lila.rating.Perf.default.add(
        lila.rating.Glicko.default.copy(
          rating = 1000 + util.Random.nextInt(1400),
          deviation = 20 + util.Random.nextInt(100)),
        DateTime.now)),
    count = lila.user.Count.default,
    enabled = true,
    roles = Nil,
    createdAt = DateTime.now,
    kid = false,
    seenAt = None,
    lang = None)

  val users = (1 to nbPlayers).map { i =>
    makeUser(s"player$i")
  }.toList

  val tour: Started = {
    val t = users.foldLeft(Tournament.make(
      createdBy = makeUser("creator"),
      clock = TournamentClock(1 * 60, 0),
      minutes = 45,
      minPlayers = 20,
      system = System.Arena,
      variant = chess.variant.Standard,
      mode = chess.Mode.Rated,
      `private` = false): Enterable) {
      case (tour, u) => (tour join u).toOption.get
    } match {
      case t: Created => t.start
      case t          => sys error "uh"
    }
    (1 to nbPrePairings).foldLeft(t) {
      case (t, _) =>
        arena.PairingSystem.createPairings(t, allUserIds).await match {
          case (pairings@(p :: ps), events) => pairings.foldLeft(
            t addPairings NonEmptyList.nel(p, ps) addEvents events
          ) {
              case (t, p) => t.updatePairing(p.gameId, _.finish(chess.Status.Draw, None, 20))
            }
          case _ => t
        }
    }
  }

  def allUserIds = AllUserIds(all = users.map(_.id), waiting = users.map(_.id))

  def runOne =
    arena.PairingSystem.createPairings(tour, allUserIds).await

  def run = (1 to nb) foreach { _ => runOne }

  "playing a game" should {
    "many times" in {
      runOne._1 must haveSize(nbPlayers / 2)
      println("warming up")
      run
      println("running tests")
      val durations = for (i ← 1 to iterations) yield {
        val start = java.lang.System.currentTimeMillis
        run
        val duration = java.lang.System.currentTimeMillis - start
        println(s"$nb pairing rounds in $duration ms")
        duration
      }
      val moveMillis = durations.sum / (iterations * nb)
      println(s"Average = $moveMillis millis per pairing")
      println(s"          ${1000 / moveMillis} pairings per second")
      true === true
    }
  }
}

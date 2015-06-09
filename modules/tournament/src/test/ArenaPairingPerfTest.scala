package lila.tournament

import org.specs2.mutable.Specification
import scalaz.NonEmptyList

import org.joda.time.DateTime

class ArenaPairingPerfTest extends Specification {

  val nb = 10
  val nbPlayers = 30
  val iterations = 10
  val nbPrePairings = 20

  val ratings = Vector(1190, 2366, 1688, 1127, 2439, 1700, 1848, 1593, 1668, 1925, 1329, 1391, 1572, 1911, 2470, 1350, 1493, 1840, 1370, 1190, 1583, 1381, 1049, 1407, 1824, 1041, 1088, 2198, 1366, 1609, 2467, 1509, 1177, 2110, 2026, 1679, 2301, 1691, 1616, 2159, 1775, 2297, 1152, 1579, 2112, 1357, 1131, 1136, 1994, 2297, 1084, 1345, 1296, 1695, 1598, 2190, 2447, 2487, 2483, 1380, 2222, 1924, 2246, 2354, 1419, 1395, 1527, 2159, 2302, 1641, 2311, 1511, 1623, 1682, 1388, 2051, 2069, 1954, 1496, 1938, 1954, 1600, 1457, 2062, 1015, 2018, 1661, 2344, 1888, 1541, 1475, 1724, 2494, 1062, 1358, 1921, 1792, 1105, 1739, 1709)

  def makeUser(username: String, rating: Int) = lila.user.User(
    id = username.toLowerCase,
    username = username,
    perfs = lila.user.Perfs.default.copy(
      bullet = lila.rating.Perf.default.add(
        lila.rating.Glicko.default.copy(
          rating = rating,
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
    makeUser(s"player$i", ratings(i))
  }.toList

  val tour: Started = {
    val t = users.foldLeft(Tournament.make(
      createdBy = makeUser("creator", 1000),
      clock = TournamentClock(1 * 60, 0),
      minutes = 45,
      system = System.Arena,
      variant = chess.variant.Standard,
      position = chess.StartingPosition.initial,
      waitMinutes = 1,
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
              case (t, p) => t.updatePairing(p.gameId, _.copy(
                status = chess.Status.Mate,
                winner = Some(chess.Color(util.Random.nextBoolean).name),
                turns = 20.some,
                perf1 = 0,
                perf2 = 0))
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
      println(s"          ${1000 / moveMillis.max(1)} pairings per second")
      true === true
    }
  }
}

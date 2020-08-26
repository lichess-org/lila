package lila.tournament
import org.specs2.mutable.Specification

private object RankingMapTest {
  def checkNames(names: Vector[String], n: Int): Boolean = {
    import cats.instances.int._
    import cats.instances.option._
    import cats.syntax.eq._
    val (v1, v2) = names.splitAt(n)
    val m        = new RankingMap(v1.toArray)
    v1.zipWithIndex.forall(t => m.get(t._1) === Some(t._2)) && v2.forall(m.get(_) === Option.empty[Int]) &&
    (0 to 100 by 10).forall(i => {
      v1.take(i).zipWithIndex.forall(t => m.betterRank(t._1, i) === Some(t._2)) && v1
        .drop(i)
        .zipWithIndex
        .forall(t => m.betterRank(t._1, i).head >= i) && v2.forall(t =>
        (m.betterRank(t, i) | Int.MaxValue) >= i
      )
    })
  }
}

class RankingMapTest extends Specification {
  import RankingMapTest.checkNames
  "RankingMap" should {
    "greatPlayers tests" in {
      val gp    = lila.common.GreatPlayer.all.keysIterator.take(550).toVector
      val listn = List.range(0, 20) ++ List(32, 48, 64, 96, 128, 256, 512, gp.size / 4, gp.size / 2, gp.size)
      listn.forall(i => checkNames(gp, i)) must beTrue
    }
  }
}

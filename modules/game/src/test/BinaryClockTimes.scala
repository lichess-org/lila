package lila.game

import org.specs2.mutable._
import org.specs2.specification._

class BinaryClockTimes extends Specification {

  def identity(clockTimes: Vector[Int]): Vector[Int] =
    BinaryFormat.clockTimes.read(BinaryFormat.clockTimes.write(clockTimes))

  "binary clock times" should {
    "read and write" in {
      val examples = List(
        Vector.empty,
        Vector(0, 0, 0, 0, 0, 0, 0, 0),
        Vector(0, 1, 2, 3, 4, 5, 6, 7),
        Vector(9, 8, 7, 6, 5, 4, 3, 2),
        Vector(1234, 54321, 123, 918273, 563719),
        Vector.tabulate(200)(n => (n - 153) * (n - 153) * (n - 32) * (n - 32)))
      examples.map(identity) must_== examples
    }
  }
}

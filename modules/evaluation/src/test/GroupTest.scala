package lila.evaluation
package grouping

import org.specs2.specification._
import org.specs2.mutable._

import scalaz.NonEmptyList

class GroupTest extends Specification {
  import lila.evaluation.grouping.Statistics._

  val games = List(
    GameGroup( //F4CFzb4J/black
      NonEmptyList(1, 20, 20, 10, 15, 30, 1, 10, 20, 40, 20, 5, 15, 400, 60, 60, 80, 100, 80, 400, 400, 20, 60, 200, 20, 30, 5, 100, 40, 30, 20, 5, 30, 300, 50, 80, 30, 100, 40),
      NonEmptyList(42, 94, 111, 0, 9, 26, 2, 84, 0, 225, 11, 0, 32, 0, 0, 0, 6, 0, 9),
      List(0),
      List(false)
    ),
    GameGroup( //7TcbR5N3/black
      NonEmptyList(1, 60, 600, 150, 600, 600, 600, 300, 600, 150, 100, 600, 600, 400, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600),
      NonEmptyList(6, 286, 54, 14, 0, 0, 6, 0, 4, 29, 1, 0, 17, 0, 0, 6, 4, 22),
      List((2*37.83783783783784).toInt),
      List(false)
    ),
    GameGroup( //0y5IrTI1/white
      NonEmptyList(1, 5, 80, 200, 20, 1, 40, 10, 20, 10, 100, 40, 40, 40, 40, 30, 60, 20, 30, 20, 40, 20, 50, 20, 100, 50, 100, 30, 150, 60, 80, 20, 30, 30, 60, 100, 400, 100, 150, 150, 600, 100, 150, 200, 150, 30, 20, 150, 300, 30, 80, 50, 60, 40, 100, 150, 150, 20, 50, 50, 40, 200, 30, 30, 5, 50, 150, 60, 20, 50, 40, 150, 40, 30, 50, 150, 20, 100, 30, 100, 20, 40, 50, 50, 40, 80, 30, 30, 20, 10, 15, 20, 15, 15, 40, 15, 15, 15, 80, 30, 60, 15, 20, 15, 30, 20, 20, 20, 10, 30, 15, 15, 40, 15, 10, 15, 20, 20, 30, 20, 30, 20, 15, 15, 30, 20, 30, 30, 20, 20, 15, 15, 15, 60, 20, 15, 30, 30, 20, 20, 20, 50, 20, 30, 80, 100, 30, 50, 15, 15, 40, 20, 15),
      NonEmptyList(0, 0, 0, 0, 0, 0, 0, 0, 994, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 335, 0, 0, 0, 0, 74, 2, 145, 189, 0, 40, 3, 67, 0, 46, 0, 677, 13, 0, 14, 36, 18, 117, 58, 89, 0, 0, 9, 10, 50, 0, 0, 0, 2, 52, 13, 113, 79, 37, 0, 50, 0, 28, 22, 46, 19, 0, 0, 26, 24),
      List((2*1.3071895424836601).toInt),
      List(false)
    ),
    GameGroup( //ruWD0agx/black
      NonEmptyList(40, 40, 50, 40, 30, 60, 60, 20, 40, 50, 30, 40, 40, 15, 50, 200, 80, 80, 60, 300, 80, 400, 100, 40, 50, 200, 80),
      NonEmptyList(352, 6, 222, 0, 0, 117, 6, 2, 25, 2, 9, 28, 18),
      List((2*0.0).toInt),
      List(false)
    ),
    GameGroup( //RYaTiTAc/black
      NonEmptyList(1, 400, 300, 600, 600, 600, 600, 600, 600, 600, 200, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600),
      NonEmptyList(7, 3, 0, 7, 7, 5, 28, 6, 23, 7, 5, 5, 0, 35),
      List((2*34.48275862068966).toInt),
      List(false)
    ),
    GameGroup( //JDEGAArH/white
      NonEmptyList(1, 600, 600, 600, 600, 600, 600, 400, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600),
      NonEmptyList(11, 4, 0, 27, 0, 48, 29, 0, 22, 24, 15, 8, 9, 4, 33, 20, 0, 12, 5, 2, 11),
      List((2*0.0).toInt),
      List(false)
    ),
    GameGroup( //u2WD3ix8/black
      NonEmptyList(1, 20, 20, 20, 20, 10, 30, 30, 15, 20, 10, 20, 15, 20, 30, 60, 15, 10, 10, 10, 15, 30, 40, 10, 15, 30, 15, 30, 40, 30, 50, 60, 20, 30, 80, 40, 60, 20, 10, 10, 15, 20, 40, 20, 20, 15, 40, 15, 20, 15, 30, 20, 1, 15, 40, 40, 10, 10, 10, 10, 20, 30, 40, 15, 20, 15, 20, 20, 15, 20, 30, 30, 20, 30, 10, 15, 15, 50, 20, 10, 100, 10, 20, 15, 15, 20, 10, 10, 15, 5, 10, 20, 20, 15),
      NonEmptyList(0, 0, 0, 0, 91, 169, 211, 0, 0, 38, 0, 93, 41, 67, 63, 0, 0, 0, 0, 0, 63, 22, 0, 9, 22, 11, 0, 0, 0, 25, 24, 36, 3, 3, 0, 12, 6, 23, 0, 45, 20, 5, 1, 0, 0, 27, 42),
      List((2*0.0).toInt),
      List(false)
    ),
    GameGroup( //C2hDXSjw/white
      NonEmptyList(100, 30, 60, 20, 400, 20, 50, 20, 80, 10, 80, 15, 80, 15, 40, 10, 80, 50, 40, 40, 50, 40, 60, 80, 60, 100, 80, 50, 60, 30, 50, 30, 50, 100, 150, 50, 80, 20, 60, 5, 40, 80, 60, 60, 50, 30, 50, 100, 50, 5, 50, 20, 100, 30, 50),
      NonEmptyList(0, 0, 0, 4, 8, 0, 0, 7, 0, 0, 0, 29, 0, 0, 14, 2, 0, 7, 0, 13, 0, 0, 8, 12, 6, 0, 0, 9),
      List((2*0.0).toInt),
      List(false)
    ),
    GameGroup( //qEkpcT5M/black
      NonEmptyList(60, 10, 5, 10, 1, 10, 5, 5, 15, 10, 10, 5, 5, 10, 5, 10, 1, 10, 1, 10, 10, 15, 5, 20, 15, 10, 30, 20, 10, 20, 15, 10, 15, 15, 15, 10, 30, 10, 20, 15, 30, 10, 10, 10, 30, 20, 30, 15),
      NonEmptyList(47, 19, 0, 1, 0, 0, 14, 99, 10, 52, 14, 31, 49, 0, 4, 4, 48, 38, 25, 9, 9, 0, 0, 9),
      List((2*0.0).toInt),
      List(false)
    ),
    GameGroup( //J8EmC0Dx/black
      NonEmptyList(50, 10, 10, 5, 10, 5, 20, 30, 10, 15, 20, 15, 15, 15, 15, 10, 5, 5, 20, 10, 20, 10, 10, 15, 20, 20),
      NonEmptyList(84, 42, 19, 4, 0, 27, 48, 5, 44, 13, 1, 45, 5),
      List((2*0.0).toInt),
      List(false)
    ),
    GameGroup( //xtEgmaku/white
      NonEmptyList(40, 1, 15, 10, 20, 10, 10, 20, 5, 60, 10, 15, 5, 20, 40, 40, 10, 50, 100, 80, 30, 5, 10, 1, 10, 20, 30, 20, 5, 20, 15, 1, 10, 15, 30, 20, 10, 40, 20, 20, 30, 15, 10, 30, 10, 20, 30, 15, 10, 30, 15, 30, 60),
      NonEmptyList(0, 0, 0, 73, 28, 11, 6, 5, 7, 3, 79, 0, 2, 0, 10, 0, 4, 0, 0, 17, 25, 3, 0, 9, 19, 27),
      List((2*1.8867924528301887).toInt),
      List(true)
    ),
    GameGroup( //kuvVv1wg/white
      NonEmptyList(40, 40, 30, 40, 10, 10, 15, 30, 10, 20, 10, 10, 10, 15, 10, 30, 10, 10, 15, 20, 10, 20, 15, 40, 10, 100, 15, 15, 15, 150, 15, 60, 15, 30, 15, 150, 15, 80, 15, 20, 20, 30, 20, 30, 15),
      NonEmptyList(0, 0, 0, 61, 3, 10, 6, 0, 0, 41, 1, 0, 0, 0, 37, 0, 56, 10, 0, 9, 3, 13),
      List((2*0.0).toInt),
      List(false)
    )
  )
  
  val gamesZip = games.zipWithIndex

  "Group similarityTo" should {
    for(game1 <- gamesZip; game2 <- gamesZip; if game1 != game2 && game1._2 < game2._2) {
      ("match Overall " + game1._2 + " to " + game2._2) in {
        game1._1.similarityTo(game2._1).matches must_=={true}
      }
    }
  }

  "Group compareMoveTimes" should {
    for(game1 <- gamesZip; game2 <- gamesZip; if game1 != game2 && game1._2 < game2._2) {
      ("match MT " + game1._2 + " AVG: " + average(game1._1.moveTimes).toInt + " SD: " + deviation(game1._1.moveTimes).toInt + " CV: " + coefVariation(game1._1.moveTimes) +
       "\n      to " + game2._2 + " AVG: " + average(game2._1.moveTimes).toInt + " SD: " + deviation(game2._1.moveTimes).toInt + " CV: " + coefVariation(game2._1.moveTimes)) in {
        game1._1.compareMoveTimes(game2._1).matches must_=={true}
      }
    }
  }

  "Group compareSfAccuracies" should {
    for(game1 <- gamesZip; game2 <- gamesZip; if game1 != game2 && game1._2 < game2._2) {
      ("match SF " + game1._2 + " AVG: " + average(game1._1.sfAccuracies).toInt + " SD:" + deviation(game1._1.sfAccuracies).toInt + 
        "\n     to " + game2._2 + " AVG: " + average(game2._1.sfAccuracies).toInt + " SD: " + deviation(game2._1.sfAccuracies).toInt) in {
        game1._1.compareSfAccuracies(game2._1).matches must_=={true}
      }
    }
  }

  "Group compareBlurRates" should {
    for(game1 <- gamesZip; game2 <- gamesZip; if game1 != game2 && game1._2 < game2._2) {
      ("match BL " + game1._2 + " to " + game2._2) in {
        game1._1.compareBlurRates(game2._1).matches must_=={true}
      }
    }
  }

  "Group compareHoldAlerts" should {
    for(game1 <- gamesZip; game2 <- gamesZip; if game1 != game2 && game1._2 < game2._2) {
      ("match HA " + game1._2 + " to " + game2._2) in {
        game1._1.compareHoldAlerts(game2._1).matches must_=={true}
      }
    }
  }
}

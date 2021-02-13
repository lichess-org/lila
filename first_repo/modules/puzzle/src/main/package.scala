package lila

import lila.rating.Glicko

package object puzzle extends PackageObject {

  private[puzzle] def logger = lila.log("puzzle")
}

package puzzle {

  case class Result(win: Boolean) extends AnyVal {
    def loss   = !win
    def glicko = if (win) Glicko.Result.Win else Glicko.Result.Loss
  }
}

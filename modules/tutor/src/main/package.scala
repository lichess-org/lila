package lila

package object tutor extends PackageObject {

  private[tutor] val logger = lila.log("tutor")

  implicit val ordering = Ordering.by[insight.TimePressure, Double](-_.value)
}

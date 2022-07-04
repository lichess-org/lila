package lila

package object tutor extends PackageObject {

  private[tutor] val logger = lila.log("tutor")

  implicit val timePressureOrdering = Ordering.by[insight.TimePressure, Double](-_.value)
}

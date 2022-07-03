package lila

package object tutor extends PackageObject {

  private[tutor] val logger = lila.log("tutor")

  implicit val timePressureOrdering       = Ordering.by[insight.TimePressure, Double](-_.value)
  implicit val globalTimePressureOrdering = Ordering.by[GlobalTimePressure, insight.TimePressure](_.value)
  implicit val defeatTimePressureOrdering = Ordering.by[DefeatTimePressure, insight.TimePressure](_.value)
}

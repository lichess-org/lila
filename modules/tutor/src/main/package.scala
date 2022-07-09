package lila

package object tutor extends PackageObject {

  private[tutor] val logger = lila.log("tutor")

  implicit val clockPercentOrdering = Ordering.by[insight.ClockPercent, Double](_.value)
}

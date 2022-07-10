package lila.tutor

import lila.analyse.AccuracyPercent
import lila.insight.ClockPercent
import lila.common.Iso

trait TutorNumber[V] {
  val iso: Iso.DoubleIso[V]
  def double(v: V)      = iso to v
  def grade(a: V, b: V) = Grade(iso.to(a), iso.to(b))
  def average(vs: Iterable[ValueCount[V]]): ValueCount[V] =
    vs.foldLeft((0d, 0)) { case ((sum, total), ValueCount(value, count)) =>
      (sum + iso.to(value) * count, total + count)
    } match {
      case (sum, total) => ValueCount(iso.from(if (total > 0) sum / total else 0), total)
    }
  def reverseCompare = new TutorNumber[V] {
    val iso                                                          = TutorNumber.this.iso
    override def grade(a: V, b: V)                                   = TutorNumber.this.grade(b, a)
    override def average(vs: Iterable[ValueCount[V]]): ValueCount[V] = TutorNumber.this.average(vs)
  }
}

object TutorNumber {

  implicit val ratioIsTutorNumber = new TutorNumber[TutorRatio] {
    val iso = Iso.double[TutorRatio](TutorRatio.fromPercent, _.percent)
  }
  implicit val doubleIsTutorNumber = new TutorNumber[Double] {
    val iso = Iso.isoIdentity[Double]
  }
  implicit val accuracyIsTutorNumber = new TutorNumber[AccuracyPercent] {
    val iso = Iso.double[AccuracyPercent](AccuracyPercent.apply, _.value)
  }
  implicit val ratingIsTutorNumber = new TutorNumber[Rating] {
    val iso                                  = Iso.double[Rating](Rating.apply, _.value)
    override def grade(a: Rating, b: Rating) = Grade((a.value - b.value) / 300)
  }
  implicit val clockPercentIsTutorNumber = new TutorNumber[ClockPercent] {
    val iso = Iso.double[ClockPercent](ClockPercent.fromPercent, _.value)
    override def grade(a: ClockPercent, b: ClockPercent) = Grade(iso.to(b), iso.to(a))
  }
}

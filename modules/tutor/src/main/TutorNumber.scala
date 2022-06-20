package lila.tutor

import lila.common.Iso

trait TutorNumber[V] {
  val iso: Iso.DoubleIso[V]
  def compare(a: V, b: V) = ValueComparison(iso.to(a), iso.to(b))
  def average(vs: Iterable[ValueCount[V]]): ValueCount[V] =
    vs.foldLeft((0d, 0)) { case ((sum, total), ValueCount(value, count)) =>
      (sum + iso.to(value), total + count)
    } match {
      case (sum, total) => ValueCount(iso.from(if (total > 0) sum / total else 0), total)
    }
}

object TutorNumber {

  implicit val ratioIsTutorNumber = new TutorNumber[TutorRatio] {
    val iso = Iso.double[TutorRatio](TutorRatio.apply, _.value)
  }
  implicit val doubleIsTutorNumber = new TutorNumber[Double] {
    val iso = Iso.isoIdentity[Double]
  }
  implicit val acplIsTutorNumber = new TutorNumber[Acpl] {
    val iso                                = Iso.double[Acpl](Acpl.apply, _.value)
    override def compare(a: Acpl, b: Acpl) = ValueComparison(-a.value, -b.value)
  }
  implicit val ratingIsTutorNumber = new TutorNumber[Rating] {
    val iso                                    = Iso.double[Rating](Rating.apply, _.value)
    override def compare(a: Rating, b: Rating) = ValueComparison((a.value - b.value) / 300)
  }
}

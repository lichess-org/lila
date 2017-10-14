package lila.report

/* Detect and discard shitty reports */
private final class ReportDiscarder {

  // true if report sucks and should be discarded
  def apply(report: Report, reporter: Reporter, getAccuracy: => Fu[Option[Int]]): Fu[Boolean] =
    report.isCheat ?? {
      getAccuracy map { _ exists discardCheatReportWithAccuracy }
    }

  // 30% accuracy => 0% discard
  // 20% accuracy => 33% discard
  // 10% accuracy => 66% discard
  // 5% accuracy => 100% discard
  private def discardCheatReportWithAccuracy(accuracy: Int): Boolean =
    accuracy <= 5 || {
      accuracy < 30 && scala.util.Random.nextInt(100) > (30 - accuracy) * 33
    }
}

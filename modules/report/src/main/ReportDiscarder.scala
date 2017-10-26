package lila.report

/* Detect and discard shitty reports */
private final class ReportDiscarder {

  // true if report sucks and should be discarded
  def apply(report: Report, reporter: Reporter, getAccuracy: => Fu[Option[Int]]): Fu[Boolean] =
    report.isCheat ?? {
      getAccuracy map { _ exists discardCheatReportWithAccuracy }
    }

  // 50% accuracy => 0% discard
  // 40% accuracy => 20% discard
  // 30% accuracy => 40% discard
  // 20% accuracy => 60% discard
  // 10% accuracy => 80% discard
  // 5% accuracy => 100% discard
  private def discardCheatReportWithAccuracy(accuracy: Int): Boolean =
    accuracy <= 5 || {
      accuracy < 50 && scala.util.Random.nextInt(100) > (50 - accuracy) * 2
    }
}

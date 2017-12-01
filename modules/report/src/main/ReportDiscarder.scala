package lila.report

/* Detect and discard shitty reports */
private final class ReportDiscarder {

  // true if report sucks and should be discarded
  def apply(candidate: Report.Candidate, getAccuracy: => Fu[Option[Int]]): Fu[Boolean] =
    candidate.isCheat ?? {
      getAccuracy map { _ exists discardCheatReportWithAccuracy }
    }

  // 50% accuracy => 0% discard
  // 40% accuracy => 75 -> 25% discard
  // 30% accuracy => 50 -> 50% discard
  // 20% accuracy => 25 -> 75% discard
  // 10% accuracy => 0  -> 100% discard
  private def discardCheatReportWithAccuracy(accuracy: Int): Boolean =
    scala.util.Random.nextInt(100) > (accuracy - 10) * 2.5
}

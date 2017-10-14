package lila.report

/* Detect and discard shitty reports */
private final class ReportDiscarder {

  // true if report sucks and should be discarded
  def apply(report: Report, reporter: Reporter, accuracy: Int): Fu[Boolean] = ???
}

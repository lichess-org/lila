package lidraughts.report

import reactivemongo.bson._

import lidraughts.db.dsl._
import lidraughts.user.{ User, UserRepo, Title }

private final class ReportScore(
    getAccuracy: ReporterId => Fu[Option[Accuracy]]
) {

  def apply(candidate: Report.Candidate): Fu[Report.Candidate.Scored] =
    getAccuracy(candidate.reporter.id) map { accuracy =>
      impl.baseScore +
        impl.accuracyScore(accuracy) +
        impl.reporterScore(candidate.reporter) +
        impl.autoScore(candidate)
    } map
      impl.fixedAutoCommPrintScore(candidate) map
      impl.commFlagScore(candidate) map { score =>
        candidate scored Report.Score(score atLeast 5 atMost 100)
      }

  private object impl {

    val baseScore = 30
    val baseScoreAboveThreshold = 50

    def accuracyScore(a: Option[Accuracy]): Double = a ?? { accuracy =>
      (accuracy.value - 50) * 0.7d
    }

    def reporterScore(r: Reporter) =
      titleScore(r.user.title) + flagScore(r.user)

    def titleScore(title: Option[Title]) =
      title.isDefined ?? 30d

    def flagScore(user: User) =
      user.lameOrTroll ?? -30d

    def autoScore(candidate: Report.Candidate) =
      candidate.isAutomatic ?? 20d

    // https://github.com/ornicar/lila/issues/4093
    // https://github.com/ornicar/lila/issues/4587
    def fixedAutoCommPrintScore(c: Report.Candidate)(score: Double): Double =
      if (c.isAutoComm) baseScore
      else if (c.isPrint || c.isCoachReview || c.isPlaybans) baseScoreAboveThreshold
      else score

    def commFlagScore(c: Report.Candidate)(score: Double): Double =
      if (c.isCommFlag) score / 2
      else score
  }

  private[report] def reset(coll: Coll)(implicit handler: BSONDocumentHandler[Report]): Fu[Int] = {
    import play.api.libs.iteratee._
    import reactivemongo.play.iteratees.cursorProducer
    coll.find($doc("open" -> true)).cursor[Report]().enumerator() |>>>
      Iteratee.foldM[Report, Int](0) {
        case (nb, report) => for {
          newAtoms <- report.atoms.map { atom =>
            candidateOf(report, atom) map {
              _.fold(atom) { scored =>
                atom.copy(score = scored.score)
              }
            }
          }.toList.sequenceFu.map(_.toNel | report.atoms)
          newReport = report.copy(atoms = newAtoms).recomputeScore
          _ <- coll.update($id(report.id), newReport)
        } yield {
          if (nb % 100 == 0) logger.info(s"Score reset $nb")
          nb + 1
        }
      }
  }

  private def candidateOf(report: Report, atom: Report.Atom): Fu[Option[Report.Candidate.Scored]] = for {
    reporter <- UserRepo byId atom.by.value map2 Reporter.apply
    suspect <- UserRepo named report.suspect.value map2 Suspect.apply
    score <- (reporter |@| suspect).tupled ?? {
      case (r, s) => apply(Report.Candidate(
        reporter = r,
        suspect = s,
        reason = report.reason,
        text = atom.text
      )) map some
    }
  } yield score
}

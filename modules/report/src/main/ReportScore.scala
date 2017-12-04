package lila.report

import reactivemongo.bson._

import lila.db.dsl._
import lila.user.{ User, UserRepo }

private final class ReportScore(
    getAccuracy: ReporterId => Fu[Option[Accuracy]]
) {

  def apply(candidate: Report.Candidate): Fu[Report.Candidate.Scored] =
    getAccuracy(candidate.reporter.id) map { accuracy =>
      impl.accuracyScore(accuracy) +
        impl.reporterScore(candidate.reporter) +
        impl.textScore(candidate.reason, candidate.text)
    } map { score =>
      candidate scored Report.Score(score atLeast 0 atMost 100)
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

  private object impl {

    def accuracyScore(a: Option[Accuracy]): Double = a ?? { accuracy =>
      (accuracy.value - 50) * 0.7d
    }

    def reporterScore(r: Reporter) =
      titleScore(r.user.title) + flagScore(r.user)

    def titleScore(title: Option[String]) =
      (title.isDefined) ?? 30d

    def flagScore(user: User) =
      (user.lameOrTroll) ?? -30d

    private val gamePattern = """lichess.org/(\w{8,12})\b""".r.pattern

    def textScore(reason: Reason, text: String) = {
      (reason == Reason.Cheat || reason == Reason.Boost) &&
        gamePattern.matcher(text).find
    } ?? 20
  }
}

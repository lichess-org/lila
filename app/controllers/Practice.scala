package controllers

import play.api.libs.json._

import lila.api.Context
import lila.app._
import lila.practice.JsonView._
import lila.practice.{ UserStudy, PracticeSection, PracticeStudy }
import lila.study.Study.WithChapter
import lila.study.{ Chapter, Study => StudyModel }
import lila.tree.Node.partitionTreeJsonWriter
import views._

object Practice extends LilaController {

  private def env = Env.practice
  private def studyEnv = Env.study

  def index = Open { implicit ctx =>
    pageHit
    env.api.get(ctx.me) flatMap { up =>
      NoCache(Ok(html.practice.index(up))).fuccess
    }
  }

  def show(sectionId: String, studySlug: String, studyId: String) = Open { implicit ctx =>
    pageHit
    OptionFuResult(env.api.getStudyWithFirstOngoingChapter(ctx.me, studyId))(showUserPractice)
  }

  def showChapter(sectionId: String, studySlug: String, studyId: String, chapterId: String) = Open { implicit ctx =>
    pageHit
    OptionFuResult(env.api.getStudyWithChapter(ctx.me, studyId, chapterId))(showUserPractice)
  }

  def showSection(sectionId: String) =
    redirectTo(sectionId)(_.studies.headOption)

  def showStudySlug(sectionId: String, studySlug: String) =
    redirectTo(sectionId)(_.studies.find(_.slug == studySlug))

  private def redirectTo(sectionId: String)(select: PracticeSection => Option[PracticeStudy]) = Open { implicit ctx =>
    env.api.structure.get.flatMap { struct =>
      struct.sections.find(_.id == sectionId).fold(notFound) { section =>
        select(section) ?? { study =>
          Redirect(routes.Practice.show(section.id, study.slug, study.id.value)).fuccess
        }
      }
    }
  }

  private def showUserPractice(us: lila.practice.UserStudy)(implicit ctx: Context) = analysisJson(us) map {
    case (analysisJson, studyJson) => NoCache(Ok(
      html.practice.show(us, lila.practice.JsonView.JsData(
        study = studyJson,
        analysis = analysisJson,
        practice = lila.practice.JsonView(us)
      ))
    ))
  }

  def chapter(studyId: String, chapterId: String) = Open { implicit ctx =>
    OptionFuResult(env.api.getStudyWithChapter(ctx.me, studyId, chapterId)) { us =>
      analysisJson(us) map {
        case (analysisJson, studyJson) => Ok(Json.obj(
          "study" -> studyJson,
          "analysis" -> analysisJson
        )) as JSON
      }
    } map NoCache
  }

  private def analysisJson(us: UserStudy)(implicit ctx: Context): Fu[(JsObject, JsObject)] = us match {
    case UserStudy(_, _, chapters, WithChapter(study, chapter), _) =>
      studyEnv.jsonView(study, chapters, chapter, ctx.me) map { studyJson =>
        val initialFen = chapter.root.fen.some
        val pov = UserAnalysis.makePov(initialFen, chapter.setup.variant)
        val baseData = Env.round.jsonView.userAnalysisJson(pov, ctx.pref, initialFen, chapter.setup.orientation, owner = false, me = ctx.me)
        val analysis = baseData ++ Json.obj(
          "treeParts" -> partitionTreeJsonWriter.writes {
            lila.study.TreeBuilder(chapter.root, chapter.setup.variant)
          },
          "practiceGoal" -> lila.practice.PracticeGoal(chapter)
        )
        (analysis, studyJson)
      }
  }

  def complete(chapterId: String, nbMoves: Int) = Auth { implicit ctx => me =>
    env.api.progress.setNbMoves(me, chapterId, lila.practice.PracticeProgress.NbMoves(nbMoves))
  }

  def reset = AuthBody { implicit ctx => me =>
    env.api.progress.reset(me) inject Redirect(routes.Practice.index)
  }

  def config = Auth { implicit ctx => me =>
    for {
      struct <- env.api.structure.get
      form <- env.api.config.form
    } yield Ok(html.practice.config(struct, form))
  }

  def configSave = SecureBody(_.StreamConfig) { implicit ctx => me =>
    implicit val req = ctx.body
    env.api.config.form.flatMap { form =>
      FormFuResult(form) { err =>
        env.api.structure.get map { html.practice.config(_, err) }
      } { text =>
        ~env.api.config.set(text).right.toOption >>-
          env.api.structure.clear >>
          Env.mod.logApi.practiceConfig(me.id) inject Redirect(routes.Practice.config)
      }
    }
  }

  private implicit def makeStudyId(id: String): StudyModel.Id = StudyModel.Id(id)
  private implicit def makeChapterId(id: String): Chapter.Id = Chapter.Id(id)
}

package controllers

import play.api.libs.json._
import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.study.Study.WithChapter
import lila.study.{ Chapter, Order, Study => StudyModel }
import views._

object Practice extends LilaController {

  private def env = Env.practice
  private def studyEnv = Env.study

  def index = Open { implicit ctx =>
    WithUserPractice { up =>
      Ok(html.practice.index(up)).fuccess
    }
  }

  def show(sectionId: String, studySlug: String, studyId: String) = Open { implicit ctx =>
    OptionFuResult(env.api.getStudy(ctx.me, studyId)) {
      case us@lila.practice.UserStudy(up, practiceStudy, WithChapter(study, chapter)) =>
        studyEnv.chapterRepo.orderedMetadataByStudy(study.id) flatMap { chapters =>
          studyEnv.api.resetIfOld(study, chapters) flatMap { study =>
            val setup = chapter.setup
            val pov = UserAnalysis.makePov(chapter.root.fen.value.some, setup.variant)
            Env.round.jsonView.userAnalysisJson(pov, ctx.pref, setup.orientation, owner = false) zip
              studyEnv.jsonView(study, chapters, chapter, ctx.me) map {
                case (baseData, studyJson) =>
                  import lila.tree.Node.partitionTreeJsonWriter
                  val analysis = baseData ++ Json.obj(
                    "treeParts" -> partitionTreeJsonWriter.writes(lila.study.TreeBuilder(chapter.root)))
                  val data = lila.practice.JsonView.JsData(
                    study = studyJson,
                    analysis = analysis,
                    practice = lila.practice.JsonView(us))
                  Ok(html.practice.show(us, data))
              }
          }
        }
    } map NoCache
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
        env.api.config.set(text).valueOr(_ => funit) >>
          env.api.structure.clear >>
          Env.mod.logApi.practiceConfig(me.id) inject Redirect(routes.Practice.config)
      }
    }
  }

  private def WithUserPractice(f: lila.practice.UserPractice => Fu[Result])(implicit ctx: Context) =
    env.api.get(ctx.me) flatMap { f(_) }

  private implicit def makeStudyId(id: String): StudyModel.Id = StudyModel.Id(id)
  private implicit def makeChapterId(id: String): Chapter.Id = Chapter.Id(id)
}

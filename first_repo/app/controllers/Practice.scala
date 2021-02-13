package controllers

import play.api.libs.json._
import scala.annotation.nowarn

import lila.api.Context
import lila.app._
import lila.practice.JsonView._
import lila.practice.{ PracticeSection, PracticeStudy, UserStudy }
import lila.study.Study.WithChapter
import lila.study.{ Chapter, Study => StudyModel }
import lila.tree.Node.partitionTreeJsonWriter
import views._

final class Practice(
    env: Env,
    userAnalysisC: => UserAnalysis
) extends LilaController(env) {

  private val api = env.practice.api

  def index =
    Open { implicit ctx =>
      pageHit
      api.get(ctx.me) flatMap { up =>
        NoCache(Ok(html.practice.index(up))).fuccess
      }
    }

  def show(
      @nowarn("cat=unused") sectionId: String,
      @nowarn("cat=unused") studySlug: String,
      studyId: String
  ) =
    Open { implicit ctx =>
      OptionFuResult(api.getStudyWithFirstOngoingChapter(ctx.me, studyId))(showUserPractice)
    }

  def showChapter(
      @nowarn("cat=unused") sectionId: String,
      @nowarn("cat=unused") studySlug: String,
      studyId: String,
      chapterId: String
  ) =
    Open { implicit ctx =>
      OptionFuResult(api.getStudyWithChapter(ctx.me, studyId, chapterId))(showUserPractice)
    }

  def showSection(sectionId: String) =
    redirectTo(sectionId)(_.studies.headOption)

  def showStudySlug(sectionId: String, studySlug: String) =
    redirectTo(sectionId)(_.studies.find(_.slug == studySlug))

  private def redirectTo(sectionId: String)(select: PracticeSection => Option[PracticeStudy]) =
    Open { implicit ctx =>
      api.structure.get.flatMap { struct =>
        struct.sections.find(_.id == sectionId).fold(notFound) { section =>
          select(section) ?? { study =>
            Redirect(routes.Practice.show(section.id, study.slug, study.id.value)).fuccess
          }
        }
      }
    }

  private def showUserPractice(us: lila.practice.UserStudy)(implicit ctx: Context) =
    analysisJson(us) map { case (analysisJson, studyJson) =>
      NoCache(
        EnableSharedArrayBuffer(
          Ok(
            html.practice.show(
              us,
              lila.practice.JsonView.JsData(
                study = studyJson,
                analysis = analysisJson,
                practice = lila.practice.JsonView(us)
              )
            )
          )
        )
      )
    }

  def chapter(studyId: String, chapterId: String) =
    Open { implicit ctx =>
      OptionFuResult(api.getStudyWithChapter(ctx.me, studyId, chapterId)) { us =>
        analysisJson(us) map { case (analysisJson, studyJson) =>
          Ok(
            Json.obj(
              "study"    -> studyJson,
              "analysis" -> analysisJson
            )
          ) as JSON
        }
      } map NoCache
    }

  private def analysisJson(us: UserStudy)(implicit ctx: Context): Fu[(JsObject, JsObject)] =
    us match {
      case UserStudy(_, _, chapters, WithChapter(study, chapter), _) =>
        env.study.jsonView(study, chapters, chapter, ctx.me) map { studyJson =>
          val initialFen = chapter.root.fen.some
          val pov        = userAnalysisC.makePov(initialFen, chapter.setup.variant)
          val baseData = env.round.jsonView
            .userAnalysisJson(
              pov,
              ctx.pref,
              initialFen,
              chapter.setup.orientation,
              owner = false,
              me = ctx.me
            )
          val analysis = baseData ++ Json.obj(
            "treeParts" -> partitionTreeJsonWriter.writes {
              lila.study.TreeBuilder(chapter.root, chapter.setup.variant)
            },
            "practiceGoal" -> lila.practice.PracticeGoal(chapter)
          )
          (analysis, studyJson)
        }
    }

  def complete(chapterId: String, nbMoves: Int) =
    Auth { implicit ctx => me =>
      api.progress.setNbMoves(me, chapterId, lila.practice.PracticeProgress.NbMoves(nbMoves))
    }

  def reset =
    AuthBody { _ => me =>
      api.progress.reset(me) inject Redirect(routes.Practice.index)
    }

  def config =
    Auth { implicit ctx => _ =>
      for {
        struct <- api.structure.get
        form   <- api.config.form
      } yield Ok(html.practice.config(struct, form))
    }

  def configSave =
    SecureBody(_.PracticeConfig) { implicit ctx => me =>
      implicit val req = ctx.body
      api.config.form.flatMap { form =>
        FormFuResult(form) { err =>
          api.structure.get map { html.practice.config(_, err) }
        } { text =>
          ~api.config.set(text).toOption >>-
            api.structure.clear() >>
            env.mod.logApi.practiceConfig(me.id) inject Redirect(routes.Practice.config)
        }
      }
    }

  implicit private def makeStudyId(id: String): StudyModel.Id = StudyModel.Id(id)
  implicit private def makeChapterId(id: String): Chapter.Id  = Chapter.Id(id)
}

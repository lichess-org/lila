package controllers

import play.api.libs.json.*

import lila.app.{ given, * }
import lila.practice.JsonView.given
import lila.practice.{ PracticeSection, PracticeStudy, UserStudy }
import lila.study.Study.WithChapter
import lila.tree.Node.partitionTreeJsonWriter
import views.*

final class Practice(
    env: Env,
    userAnalysisC: => UserAnalysis
) extends LilaController(env):

  private val api = env.practice.api

  def index = Open:
    pageHit
    Ok.pageAsync:
      api.get(ctx.me) map { html.practice.index(_) }
    .map(_.noCache)

  def show(sectionId: String, studySlug: String, studyId: StudyId) = Open:
    Found(api.getStudyWithFirstOngoingChapter(ctx.me, studyId))(showUserPractice)

  def showChapter(
      sectionId: String,
      studySlug: String,
      studyId: StudyId,
      chapterId: StudyChapterId
  ) = Open:
    Found(api.getStudyWithChapter(ctx.me, studyId, chapterId))(showUserPractice)

  def showSection(sectionId: String) =
    redirectTo(sectionId)(_.studies.headOption)

  def showStudySlug(sectionId: String, studySlug: String) =
    redirectTo(sectionId)(_.studies.find(_.slug == studySlug))

  private def redirectTo(sectionId: String)(select: PracticeSection => Option[PracticeStudy]) = Open:
    api.structure.get.flatMap: struct =>
      Found(struct.sections.find(_.id == sectionId)): section =>
        select(section).so: study =>
          Redirect(routes.Practice.show(section.id, study.slug, study.id))

  private def showUserPractice(us: lila.practice.UserStudy)(using Context) =
    Ok.pageAsync:
      analysisJson(us).map: (analysisJson, studyJson) =>
        html.practice
          .show(
            us,
            lila.practice.JsonView.JsData(
              study = studyJson,
              analysis = analysisJson,
              practice = lila.practice.JsonView(us)
            )
          )
    .map:
        _.noCache.enableSharedArrayBuffer.withCanonical(s"${us.url}/${us.study.chapter.id}")

  def chapter(studyId: StudyId, chapterId: StudyChapterId) = Open:
    Found(api.getStudyWithChapter(ctx.me, studyId, chapterId)): us =>
      analysisJson(us).map: (analysisJson, studyJson) =>
        JsonOk(
          Json.obj(
            "study"    -> studyJson,
            "analysis" -> analysisJson
          )
        ).noCache

  private def analysisJson(us: UserStudy)(using Context): Fu[(JsObject, JsObject)] =
    us match
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
              owner = false
            )
          val analysis = baseData ++ Json.obj(
            "treeParts" -> partitionTreeJsonWriter.writes {
              lila.study.TreeBuilder(chapter.root, chapter.setup.variant)
            },
            "practiceGoal" -> lila.practice.PracticeGoal(chapter)
          )
          (analysis, studyJson)
        }

  def complete(chapterId: StudyChapterId, nbMoves: Int) = Auth { ctx ?=> me ?=>
    api.progress.setNbMoves(me, chapterId, lila.practice.PracticeProgress.NbMoves(nbMoves)) inject NoContent
  }

  def reset = AuthBody { _ ?=> me ?=>
    api.progress.reset(me) inject Redirect(routes.Practice.index)
  }

  def config = Secure(_.PracticeConfig) { ctx ?=> _ ?=>
    for
      struct <- api.structure.get
      form   <- api.config.form
      page   <- renderPage(html.practice.config(struct, form))
    yield Ok(page)
  }

  def configSave = SecureBody(_.PracticeConfig) { ctx ?=> me ?=>
    api.config.form.flatMap: form =>
      FormFuResult(form) { err =>
        renderAsync:
          api.structure.get.map(html.practice.config(_, err))
      } { text =>
        ~api.config.set(text).toOption >>
          env.mod.logApi.practiceConfig andDo
          api.structure.clear() inject
          Redirect(routes.Practice.config)
      }
  }

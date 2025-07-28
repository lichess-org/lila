package controllers

import scala.annotation.nowarn
import play.api.libs.json.*

import lila.app.{ *, given }
import lila.practice.JsonView.given
import lila.practice.{ PracticeSection, PracticeStudy, UserStudy }
import lila.study.Study.WithChapter
import lila.tree.Node.partitionTreeJsonWriter

final class Practice(
    env: Env,
    userAnalysisC: => UserAnalysis
) extends LilaController(env):

  private val api = env.practice.api

  def index = OpenOrScoped(_.Web.Mobile):
    negotiate(
      html =
        pageHit
        Ok.async(api.get(ctx.me).map(views.practice.index)).map(_.noCache)
      ,
      json = api.get(ctx.me).map(lila.practice.JsonView.api).map(JsonOk)
    )

  def show(
      @nowarn sectionId: String,
      @nowarn studySlug: String,
      studyId: StudyId
  ) = Open:
    Found(api.getStudyWithFirstOngoingChapter(ctx.me, studyId))(showUserPractice)

  def showChapter(
      @nowarn sectionId: String,
      @nowarn studySlug: String,
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
    Ok.async:
      analysisJson(us).map: (analysisJson, studyJson) =>
        views.practice
          .show(
            us,
            lila.practice.JsonView.JsData(
              study = studyJson,
              analysis = analysisJson,
              practice = lila.practice.JsonView(us)
            )
          )
    .map:
        _.noCache.enforceCrossSiteIsolation.withCanonical(s"${us.url}/${us.study.chapter.id}")

  def chapter(studyId: StudyId, chapterId: StudyChapterId) = Open:
    Found(api.getStudyWithChapter(ctx.me, studyId, chapterId)): us =>
      analysisJson(us).map: (analysisJson, studyJson) =>
        JsonOk(
          Json.obj(
            "study" -> studyJson,
            "analysis" -> analysisJson
          )
        ).noCache

  private def analysisJson(us: UserStudy)(using Context): Fu[(JsObject, JsObject)] = us match
    case UserStudy(_, _, chapters, WithChapter(study, chapter), _) =>
      for
        studyJson <- env.study.jsonView.full(study, chapter, chapters.some, none, withMembers = false)
        initialFen = chapter.root.fen.some
        pov = userAnalysisC.makePov(initialFen, chapter.setup.variant)
        baseData = env.round.jsonView
          .userAnalysisJson(
            pov,
            ctx.pref,
            initialFen,
            chapter.setup.orientation,
            owner = false
          )
        analysis = baseData ++ Json.obj(
          "treeParts" -> partitionTreeJsonWriter.writes {
            lila.study.TreeBuilder(chapter.root, chapter.setup.variant)
          },
          "practiceGoal" -> lila.practice.PracticeGoal(chapter)
        )
        analysisJson <- env.analyse.externalEngine.withExternalEngines(analysis)
      yield (analysisJson, studyJson)

  def complete(chapterId: StudyChapterId, nbMoves: Int) = AuthOrScoped(_.Web.Mobile) { ctx ?=> me ?=>
    api.progress.setNbMoves(me, chapterId, lila.practice.PracticeProgress.NbMoves(nbMoves)).inject(NoContent)
  }

  def reset = AuthBody { _ ?=> me ?=>
    api.progress.reset(me).inject(Redirect(routes.Practice.index))
  }

  def config = Secure(_.PracticeConfig) { ctx ?=> _ ?=>
    for
      struct <- api.structure.get
      form <- api.config.form
      page <- renderPage(views.practice.config(struct, form))
    yield Ok(page)
  }

  def configSave = SecureBody(_.PracticeConfig) { ctx ?=> me ?=>
    api.config.form.flatMap: form =>
      FormFuResult(form) { err =>
        renderAsync:
          api.structure.get.map(views.practice.config(_, err))
      } { text =>
        for
          _ <- ~api.config.set(text).toOption
          _ <- env.mod.logApi.practiceConfig
          _ = api.structure.clear()
        yield Redirect(routes.Practice.config)
      }
  }

package lila.study
package ui

import lila.common.String.removeMultibyteSymbols
import lila.core.study.Order
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class StudyBits(helpers: Helpers):
  import helpers.{ *, given }

  def orderSelect(order: Order, active: String, url: Order => Call)(using Context) =
    val orders =
      if active == "all" then Orders.withoutSelector
      else if active.startsWith("topic") then Orders.list
      else Orders.withoutMine
    lila.ui.bits.mselect(
      "orders",
      span(Orders.name(order)()),
      orders.map: o =>
        a(href := url(o), cls := (order == o).option("current"))(Orders.name(o)())
    )

  def newForm()(using Context) =
    postForm(cls := "new-study", action := routes.Study.create)(
      submitButton(
        cls      := "button button-green",
        dataIcon := Icon.PlusButton,
        title    := trans.study.createStudy.txt()
      )
    )

  def authLinks(active: String, order: Order)(using Context) =
    def activeCls(c: String) = cls := (c == active).option("active")
    frag(
      a(activeCls("mine"), href := routes.Study.mine(order))(trans.study.myStudies()),
      a(activeCls("mineMember"), href := routes.Study.mineMember(order))(
        trans.study.studiesIContributeTo()
      ),
      a(activeCls("minePublic"), href := routes.Study.minePublic(order))(trans.study.myPublicStudies()),
      a(activeCls("minePrivate"), href := routes.Study.minePrivate(order))(
        trans.study.myPrivateStudies()
      ),
      a(activeCls("mineLikes"), href := routes.Study.mineLikes(order))(trans.study.myFavoriteStudies())
    )

  def widget(s: Study.WithChaptersAndLiked, tag: Tag = h2)(using ctx: Context) =
    frag(
      a(cls := "overlay", href := routes.Study.show(s.study.id), title := s.study.name),
      div(cls := "top")(
        div(cls := "study__icon")(
          s.study.flair
            .map(iconFlair)
            .orElse(userFlairSync(s.study.ownerId))
            .getOrElse(iconTag(Icon.StudyBoard))
        ),
        div(
          tag(cls := "study-name")(s.study.name),
          span(
            (!s.study.isPublic).option(
              frag(
                iconTag(Icon.Padlock)(cls := "private", ariaTitle(trans.study.`private`.txt())),
                " "
              )
            ),
            iconTag(if s.liked then Icon.Heart else Icon.HeartOutline),
            " ",
            s.study.likes.value,
            " • ",
            titleNameOrId(s.study.ownerId),
            " • ",
            momentFromNow(s.study.createdAt)
          )
        )
      ),
      div(cls := "body")(
        ol(cls := "chapters")(
          s.chapters.map: name =>
            li(cls := "text", dataIcon := Icon.DiscBigOutline)(
              if ctx.userId.exists(s.study.isMember) then name
              else removeMultibyteSymbols(name.value)
            )
        ),
        ol(cls := "members")(
          s.study.members.members.values
            .take(Study.previewNbMembers)
            .map: m =>
              li(cls := "text", dataIcon := (if m.canContribute then Icon.RadioTower else Icon.Eye))(
                titleNameOrId(m.id)
              )
            .toList
        )
      )
    )

  val i18nKeys =
    import trans.{ site, study as trs }
    List(
      site.name,
      site.white,
      site.black,
      site.variant,
      site.clearBoard,
      site.startPosition,
      site.cancel,
      site.chat,
      trs.addNewChapter,
      trs.importFromChapterX,
      trs.addMembers,
      trs.inviteToTheStudy,
      trs.pleaseOnlyInvitePeopleYouKnow,
      trs.searchByUsername,
      trs.spectator,
      trs.contributor,
      trs.kick,
      trs.leaveTheStudy,
      trs.youAreNowAContributor,
      trs.youAreNowASpectator,
      trs.pgnTags,
      trs.like,
      trs.unlike,
      trs.topics,
      trs.manageTopics,
      trs.newTag,
      trs.commentThisPosition,
      trs.commentThisMove,
      trs.annotateWithGlyphs,
      trs.theChapterIsTooShortToBeAnalysed,
      trs.onlyContributorsCanRequestAnalysis,
      trs.getAFullComputerAnalysis,
      trs.makeSureTheChapterIsComplete,
      trs.allSyncMembersRemainOnTheSamePosition,
      trs.shareChanges,
      trs.playing,
      trs.showEvalBar,
      trs.first,
      trs.previous,
      trs.next,
      trs.last,
      trs.nextChapter,
      trs.shareAndExport,
      trs.cloneStudy,
      trs.studyPgn,
      trs.downloadAllGames,
      trs.chapterPgn,
      trs.copyChapterPgn,
      trs.downloadGame,
      trs.studyUrl,
      trs.currentChapterUrl,
      trs.youCanPasteThisInTheForumToEmbed,
      trs.startAtInitialPosition,
      trs.startAtX,
      trs.embedInYourWebsite,
      trs.readMoreAboutEmbedding,
      trs.onlyPublicStudiesCanBeEmbedded,
      trs.open,
      trs.xBroughtToYouByY,
      trs.studyNotFound,
      trs.editChapter,
      trs.newChapter,
      trs.orientation,
      trs.analysisMode,
      trs.pinnedChapterComment,
      trs.saveChapter,
      trs.clearAnnotations,
      trs.clearVariations,
      trs.deleteChapter,
      trs.deleteThisChapter,
      trs.clearAllCommentsInThisChapter,
      trs.rightUnderTheBoard,
      trs.noPinnedComment,
      trs.normalAnalysis,
      trs.hideNextMoves,
      trs.interactiveLesson,
      trs.chapterX,
      trs.empty,
      trs.startFromInitialPosition,
      trs.editor,
      trs.startFromCustomPosition,
      trs.loadAGameByUrl,
      trs.loadAPositionFromFen,
      trs.loadAGameFromPgn,
      trs.automatic,
      trs.urlOfTheGame,
      trs.loadAGameFromXOrY,
      trs.createChapter,
      trs.createStudy,
      trs.editStudy,
      trs.visibility,
      trs.public,
      trs.`private`,
      trs.unlisted,
      trs.inviteOnly,
      trs.allowCloning,
      trs.nobody,
      trs.onlyMe,
      trs.contributors,
      trs.members,
      trs.everyone,
      trs.enableSync,
      trs.yesKeepEveryoneOnTheSamePosition,
      trs.noLetPeopleBrowseFreely,
      trs.pinnedStudyComment,
      trs.start,
      trs.save,
      trs.clearChat,
      trs.deleteTheStudyChatHistory,
      trs.deleteStudy,
      trs.confirmDeleteStudy,
      trs.whereDoYouWantToStudyThat,
      trs.nbChapters,
      trs.nbGames,
      trs.nbMembers,
      trs.pasteYourPgnTextHereUpToNbGames
    )

  val gamebookPlayKeys =
    List(
      trans.study.back,
      trans.study.playAgain,
      trans.study.nextChapter,
      trans.site.retry,
      trans.study.whatWouldYouPlay,
      trans.study.youCompletedThisLesson
    )

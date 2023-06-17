package views.html.study

import play.api.i18n.Lang

import lila.app.templating.Environment.{ given, * }
import lila.i18n.{ I18nKeys as trans }

object jsI18n:

  def apply()(using Lang) =
    views.html.board.userAnalysisI18n(withAdvantageChart = true) ++
      i18nJsObject(i18nKeys ++ gamebookPlayKeys)

  def embed(chapter: lila.study.Chapter)(using Lang) =
    views.html.board.userAnalysisI18n() ++ chapter.isGamebook.so {
      i18nJsObject(gamebookPlayKeys)
    }

  val i18nKeys =
    import trans.study.*
    List(
      trans.name,
      trans.white,
      trans.black,
      trans.variant,
      trans.clearBoard,
      trans.startPosition,
      trans.cancel,
      trans.chat,
      addNewChapter,
      importFromChapterX,
      addMembers,
      inviteToTheStudy,
      pleaseOnlyInvitePeopleYouKnow,
      searchByUsername,
      spectator,
      contributor,
      kick,
      leaveTheStudy,
      youAreNowAContributor,
      youAreNowASpectator,
      pgnTags,
      like,
      unlike,
      topics,
      manageTopics,
      newTag,
      commentThisPosition,
      commentThisMove,
      annotateWithGlyphs,
      theChapterIsTooShortToBeAnalysed,
      onlyContributorsCanRequestAnalysis,
      getAFullComputerAnalysis,
      makeSureTheChapterIsComplete,
      allSyncMembersRemainOnTheSamePosition,
      shareChanges,
      playing,
      first,
      previous,
      next,
      last,
      nextChapter,
      shareAndExport,
      cloneStudy,
      studyPgn,
      downloadAllGames,
      chapterPgn,
      copyChapterPgn,
      copyChapterPgnDescription,
      downloadGame,
      studyUrl,
      currentChapterUrl,
      youCanPasteThisInTheForumToEmbed,
      startAtInitialPosition,
      startAtX,
      embedInYourWebsite,
      readMoreAboutEmbedding,
      onlyPublicStudiesCanBeEmbedded,
      open,
      xBroughtToYouByY,
      studyNotFound,
      editChapter,
      newChapter,
      orientation,
      analysisMode,
      pinnedChapterComment,
      saveChapter,
      clearAnnotations,
      clearVariations,
      deleteChapter,
      deleteThisChapter,
      clearAllCommentsInThisChapter,
      rightUnderTheBoard,
      noPinnedComment,
      normalAnalysis,
      hideNextMoves,
      interactiveLesson,
      chapterX,
      empty,
      startFromInitialPosition,
      editor,
      startFromCustomPosition,
      loadAGameByUrl,
      loadAPositionFromFen,
      loadAGameFromPgn,
      automatic,
      urlOfTheGame,
      loadAGameFromXOrY,
      createChapter,
      createStudy,
      editStudy,
      visibility,
      public,
      `private`,
      unlisted,
      inviteOnly,
      allowCloning,
      nobody,
      onlyMe,
      contributors,
      members,
      everyone,
      enableSync,
      yesKeepEveryoneOnTheSamePosition,
      noLetPeopleBrowseFreely,
      pinnedStudyComment,
      start,
      save,
      clearChat,
      deleteTheStudyChatHistory,
      deleteStudy,
      confirmDeleteStudy,
      whereDoYouWantToStudyThat,
      nbChapters,
      nbGames,
      nbMembers,
      pasteYourPgnTextHereUpToNbGames
    )

  val gamebookPlayKeys =
    import trans.study.*
    List(
      back,
      playAgain,
      nextChapter,
      trans.retry,
      whatWouldYouPlay,
      youCompletedThisLesson
    )

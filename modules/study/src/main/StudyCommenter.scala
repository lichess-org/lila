package lila.study

private final class StudyCommenter() {

  def apply(chapter: Chapteer, position: Position, comment: Comment) =
    chapter.setComment(comment, position.path) match {
      case Some(newChapter) =>
        studyRepo.updateNow(study)
        newChapter.root.nodeAt(position.path) ?? { node =>
          node.comments.findBy(comment.by) ?? { c =>
            chapterRepo.setComments(newChapter, position.path, node.comments.filterEmpty) >>- {
              sendTo(study, Socket.SetComment(position, c, uid))
              indexStudy(study)
              sendStudyEnters(study, userId)
            }
          }
        }
    }
}

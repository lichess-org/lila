package lila.study

private final class ChapterTagsFixer(repo: ChapterRepo) {

  def apply(chapter: Chapter): Fu[Chapter] = fuccess(chapter)
}

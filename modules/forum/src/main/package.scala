package lila

package object forum extends PackageObject {

  private[forum] def teamSlug(id: String) = s"team-$id"

  private[forum] val logger = lila.log("forum")

  private[forum] val publicCategIdsTranslated = Map(
    "general-shogi-discussion" -> lila.i18n.I18nKeys.generalShogiDiscussionForum,
    "game-analysis"            -> lila.i18n.I18nKeys.gameAnalysisForum,
    "lishogi-feedback"         -> lila.i18n.I18nKeys.lishogiFeedbackForum,
    "off-topic-discussion"     -> lila.i18n.I18nKeys.offTopicDiscussionForum
  )
  private[forum] val publicCategIds = publicCategIdsTranslated.keys.toList
}

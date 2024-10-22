package lila.core.i18n

enum I18nModule:
  case site, arena, emails, learn, activity, coordinates, study, `class`, contact, appeal, patron, coach,
    broadcast, streamer, tfa, settings, preferences, team, perfStat, search, tourname, faq, lag, swiss,
    puzzle, puzzleTheme, challenge, storm, ublog, insight, keyboardMove, timeago, oauthScope, dgt,
    voiceCommands, onboarding, features
object I18nModule:
  type Selector = I18nModule.type => I18nModule

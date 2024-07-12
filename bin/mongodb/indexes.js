db.clas_clas.createIndex({ teachers: 1, viewedAt: -1 });
db.event.createIndex({ startsAt: 1 });
db.picfit_image.createIndex({ rel: 1 }, { unique: true });
db.tutor_report.createIndex({ at: -1 });
db.swiss_pairing.createIndex({ s: 1, p: 1, r: 1 });
db.swiss_pairing.createIndex({ t: 1 }, { partialFilterExpression: { t: true } });
db.oauth2_authorization.createIndex({ expires: 1 }, { expireAfterSeconds: 0 });
db.trophy.createIndex({ user: 1 });
db.simul.createIndex({ status: 1, createdAt: -1 });
db.simul.createIndex({ hostSeenAt: -1 }, { partialFilterExpression: { status: 10, featurable: true } });
db.simul.createIndex({ finishedAt: -1, featurable: 1 });
db.simul.createIndex({ hostId: 1 });
db.ublog_post.createIndex({ blog: 1, 'live.at': -1 }, { partialFilterExpression: { live: true } });
db.ublog_post.createIndex({ blog: 1, 'created.at': -1 }, { partialFilterExpression: { live: false } });
db.ublog_post.createIndex({ rank: -1 }, { partialFilterExpression: { live: true } });
db.ublog_post.createIndex({ likers: 1, rank: -1 }, { partialFilterExpression: { live: true } });
db.ublog_post.createIndex({ language: 1, rank: -1 }, { partialFilterExpression: { live: true } });
db.ublog_post.createIndex({ topics: 1, rank: -1 }, { partialFilterExpression: { live: true } });
db.ublog_post.createIndex({ topics: 1, 'lived.at': -1 }, { partialFilterExpression: { live: true } });
db.ublog_post.createIndex({ likers: 1, 'live.at': -1 }, { partialFilterExpression: { live: true } });
db.ublog_post.createIndex({ prismicId: 1 }, { partialFilterExpression: { prismicId: { $exists: 1 } } });
db.report2.createIndex({ room: 1, score: -1 }, { partialFilterExpression: { open: true } });
db.report2.createIndex(
  { 'inquiry.mod': 1 },
  { partialFilterExpression: { 'inquiry.mod': { $exists: true } } },
);
db.report2.createIndex({ user: 1 });
db.report2.createIndex({ 'atoms.by': 1 });
db.report2.createIndex({ room: 1, 'atoms.0.at': -1 });
db.report2.createIndex({ 'atoms.0.at': -1 }, { background: 1 });
db.report2.createIndex({ 'done.at': -1 }, { partialFilterExpression: { open: false } });
db.firewall.createIndex({ date: 1 }, { expireAfterSeconds: 7776000, background: true });
db.timeline_entry.createIndex({ users: 1, date: -1 });
db.timeline_entry.createIndex({ typ: 1, date: -1 });
db.notify.createIndex({ notifies: 1, read: 1, createdAt: -1 });
db.notify.createIndex({ createdAt: 1 }, { expireAfterSeconds: 2592000 });
db.tournament_pairing.createIndex({ tid: 1, d: -1 });
db.tournament_pairing.createIndex({ tid: 1, u: 1, d: -1 });
db.tournament_pairing.createIndex({ tid: 1 }, { partialFilterExpression: { s: { $lt: 30 } }, background: 1 });
db.round_history.createIndex({ d: 1 }, { expireAfterSeconds: 3600 });
db.relay_tour.createIndex(
  { active: 1, tier: 1 },
  { partialFilterExpression: { active: true, tier: { $exists: true } } },
);
db.relay_tour.createIndex({ syncedAt: -1 }, { partialFilterExpression: { tier: { $exists: true } } });
db.relay_tour.createIndex(
  { _fts: 'text', _ftsx: 1 },
  {
    weights: { description: 1, name: 3 },
    partialFilterExpression: { tier: { $exists: true } },
    default_language: 'english',
    language_override: 'language',
    textIndexVersion: 3,
  },
);
db.relay_tour.createIndex({ ownerId: 1, syncedAt: -1 });
db.relay_tour.createIndex({ ownerId: 1, createdAt: -1 });
db.relay_tour.createIndex({ subscribers: 1, createdAt: -1 });
db.relation_subs.createIndex({ s: 1 });
db.round_alarm.createIndex({ ringsAt: 1 });
db.round_alarm.createIndex({ expiresAt: 1 }, { expireAfterSeconds: 0 });
db.irwin_request.createIndex({ startedAt: 1 }, { expireAfterSeconds: 3600 });
db.irwin_request.createIndex({ priority: 1 });
db.irwin_request.createIndex({ createdAt: 1 }, { expireAfterSeconds: 1296000 });
db.tournament2.createIndex({ status: 1 });
db.tournament2.createIndex({ startsAt: 1 });
db.tournament2.createIndex({ 'schedule.freq': 1, startsAt: -1 }, { background: true });
db.tournament2.createIndex(
  { status: 1, startsAt: 1 },
  { partialFilterExpression: { status: 10 }, background: 1 },
);
db.tournament2.createIndex(
  { forTeams: 1, startsAt: -1 },
  { partialFilterExpression: { forTeams: { $exists: 1 } } },
);
db.tournament2.createIndex(
  { createdBy: 1, startsAt: -1, status: 1 },
  { partialFilterExpression: { createdBy: { $exists: true } } },
);
db.relation.createIndex({ u1: 1 });
db.relation.createIndex({ u2: 1 });
db.fide_player.createIndex({ token: 1 });
db.fide_player.createIndex({ fed: 1, standard: -1 });
db.fide_player.createIndex({ fed: 1, rapid: -1 });
db.fide_player.createIndex({ fed: 1, blitz: -1 });
db.fide_player.createIndex({ standard: -1 });
db.fide_player.createIndex(
  { _fts: 'text', _ftsx: 1, standard: -1 },
  { weights: { token: 1 }, default_language: 'english', language_override: 'language', textIndexVersion: 3 },
);
db.note.createIndex({ to: 1, date: -1 }, { background: 1 });
db.note.createIndex(
  { _fts: 'text', _ftsx: 1, dox: 1, date: -1 },
  {
    partialFilterExpression: { s: true },
    weights: { from: 1, text: 1, to: 1 },
    default_language: 'english',
    language_override: 'language',
    textIndexVersion: 3,
  },
);
db.irwin_report.createIndex({ date: -1 });
db.user4.createIndex({ 'count.game': -1 });
db.user4.createIndex({ title: 1 }, { sparse: true });
db.user4.createIndex({ email: 1 }, { unique: true, sparse: 1 });
db.user4.createIndex({ roles: 1 }, { background: 1, partialFilterExpression: { roles: { $exists: 1 } } });
db.user4.createIndex({ prevEmail: 1 }, { sparse: 1, background: 1 });
db.user4.createIndex({ eraseAt: 1 }, { partialFilterExpression: { eraseAt: { $exists: true } } });
db.f_topic.createIndex({ categId: 1, troll: 1 });
db.f_topic.createIndex({ categId: 1, updatedAt: -1, troll: 1 });
db.f_topic.createIndex({ categId: 1, slug: 1 });
db.f_topic.createIndex({ categId: 1 }, { partialFilterExpression: { sticky: true }, background: true });
db.seek_archive.createIndex({ archivedAt: 1 }, { expireAfterSeconds: 604800 });
db.seek_archive.createIndex({ gameId: 1 });
db.swiss_player.createIndex({ s: 1, c: -1 });
db.print_ban.createIndex({ date: 1 }, { expireAfterSeconds: 7776000 });
db.appeal.createIndex({ status: 1, lastUnrepliedAt: 1 });
db.challenge.createIndex({ status: 1, 'challenger.id': 1, createdAt: 1 });
db.challenge.createIndex({ status: 1, 'destUser.id': 1, createdAt: 1 });
db.challenge.createIndex({ expiresAt: 1 });
db.challenge.createIndex(
  { seenAt: 1 },
  { partialFilterExpression: { status: 10, 'timeControl.l': { $exists: true } } },
);
db.cms_page.createIndex({ key: 1 });
db.cms_page.createIndex({ key: 1, language: 1 }, { unique: true });
db.donation.createIndex({ payPalTnx: 1 }, { unique: true, sparse: 1 });
db.donation.createIndex({ userId: 1 });
db.donation.createIndex({ date: -1 });
db.donation.createIndex({ gross: -1 });
db.player_assessment.createIndex({ userId: 1, date: -1 }, { background: 1 });
db.player_assessment.createIndex({ date: 1 }, { expireAfterSeconds: 15552000, background: true });
db.fishnet_analysis.createIndex({ 'sender.system': 1, createdAt: 1 });
db.fishnet_analysis.createIndex({ 'game.id': 1 }, { background: true });
db.fishnet_analysis.createIndex({ 'sender.userId': 1 }, { background: 1 });
db.fishnet_analysis.createIndex({ 'sender.ip': 1 }, { background: 1 });
db.fishnet_analysis.createIndex({ 'sender.system': 1 }, { background: 1 });
db.fishnet_analysis.createIndex(
  { acquired: 1 },
  { partialFilterExpression: { acquired: { $exists: true } }, background: 1 },
);
db.bookmark.createIndex({ g: 1 });
db.bookmark.createIndex({ u: 1 });
db.bookmark.createIndex({ u: 1, d: -1 });
db.f_categ.createIndex({ team: 1 });
db.relay_group.createIndex({ tours: 1 });
db.seek.createIndex({ createdAt: -1 }, { expireAfterSeconds: 86400 });
db.msg_msg.createIndex({ tid: 1, date: -1 }, { background: 1 });
db.team.createIndex({ enabled: 1, nbMembers: -1 });
db.team.createIndex({ createdAt: -1 });
db.team.createIndex({ createdBy: 1 });
db.team.createIndex({ leaders: 1 }, { background: 1 });
db.swiss.createIndex({ teamId: 1, startsAt: 1 });
db.swiss.createIndex({ nextRoundAt: 1 }, { partialFilterExpression: { nextRoundAt: { $exists: true } } });
db.swiss.createIndex(
  { featurable: 1 },
  { partialFilterExpression: { featurable: true, 'settings.i': { $lte: 600 } } },
);
db.coach_review.createIndex({ coachId: 1 });
db.analysis_requester.createIndex({ total: -1 });
db.plan_patron.createIndex(
  { 'stripe.customerId': 1 },
  { partialFilterExpression: { 'stripe.customerId': { $exists: true } } },
);
db.plan_patron.createIndex({ 'free.by': 1 }, { partialFilterExpression: { 'free.by': { $exists: true } } });
db.plan_patron.createIndex({ expiresAt: 1 }, { partialFilterExpression: { expiresAt: { $exists: true } } });
db.plan_patron.createIndex(
  { 'payPalCheckout.subscriptionId': 1 },
  { partialFilterExpression: { 'payPalCheckout.subscriptionId': { $exists: true } } },
);
db.notify_pref.createIndex(
  { correspondenceEmail: 1 },
  { partialFilterExpression: { correspondenceEmail: true } },
);
db.modlog.createIndex({ user: 1, date: -1 });
db.modlog.createIndex({ date: -1 });
db.modlog.createIndex({ mod: 1, date: -1 }, { partialFilterExpression: { human: true } });
db.modlog.createIndex({ index: 1, details: 1 }, { partialFilterExpression: { index: 'team' } });
db.chat_timeout.createIndex({ expiresAt: -1 }, { partialFilterExpression: { expiresAt: { $exists: 1 } } });
db.chat_timeout.createIndex(
  { chat: 1, expiresAt: -1 },
  { partialFilterExpression: { expiresAt: { $exists: 1 } } },
);
db.chat_timeout.createIndex({ user: 1, createdAt: -1 }, { background: 1 });
db.daily_feed.createIndex({ at: -1 });
db.tournament_leaderboard.createIndex({ t: 1 });
db.tournament_leaderboard.createIndex({ u: 1, d: -1 });
db.tournament_leaderboard.createIndex({ u: 1, w: 1 });
db.coach.createIndex({ 'user.seenAt': -1 });
db.coach.createIndex({ 'user.rating': -1 });
db.coach.createIndex({ nvReviews: -1 });
db.streamer.createIndex({ liveAt: -1 });
db.streamer.createIndex(
  { 'approval.granted': 1, listed: 1 },
  { partialFilterExpression: { 'approval.granted': true, listed: true } },
);
db.streamer.createIndex(
  { 'approval.requested': 1 },
  { partialFilterExpression: { 'approval.requested': true, 'approval.ignored': false } },
);
db.relay.createIndex({ startsAt: 1 }, { partialFilterExpression: { startsAt: { $exists: 1 } } });
db.relay.createIndex({ startedAt: 1 }, { partialFilterExpression: { startedAt: { $exists: 1 } } });
db.relay.createIndex({ 'sync.until': 1 }, { partialFilterExpression: { 'sync.until': { $exists: 1 } } });
db.relay.createIndex({ tourId: 1 });
db.relay.createIndex(
  { 'sync.upstream.roundIds': 1 },
  { partialFilterExpression: { 'sync.upstream.roundIds': { $exists: 1 } } },
);
db.oauth2_access_token.createIndex({ userId: 1 });
db.oauth2_access_token.createIndex({ expires: 1 }, { expireAfterSeconds: 0 });
db.cache.createIndex({ e: 1 }, { expireAfterSeconds: 0 });
db.forecast.createIndex({ date: 1 }, { expireAfterSeconds: 1296000 });
db.msg_thread.createIndex({ users: 1, 'lastMsg.date': -1 }, { background: 1 });
db.msg_thread.createIndex(
  { users: 1 },
  { partialFilterExpression: { 'lastMsg.read': false }, background: 1 },
);
db.msg_thread.createIndex({ users: 1, 'maskWith.date': -1 });
db.video.createIndex({ 'metadata.likes': -1 });
db.video.createIndex({ tags: 1, 'metadata.likes': -1 });
db.video.createIndex({ author: 1, 'metadata.likes': -1 });
db.video.createIndex(
  { _fts: 'text', _ftsx: 1 },
  {
    weights: { author: 3, description: 1, tags: 5, title: 10 },
    default_language: 'english',
    language_override: 'language',
    textIndexVersion: 2,
  },
);
db.team_request.createIndex({ team: 1 });
db.team_request.createIndex({ user: 1 });
db.team_request.createIndex({ team: 1, date: -1 });
db.team_request.createIndex({ date: 1 }, { expireAfterSeconds: 31536000 });
db.tournament_player.createIndex({ tid: 1, m: -1 });
db.tournament_player.createIndex(
  { tid: 1, t: 1, m: -1 },
  { partialFilterExpression: { t: { $exists: true } } },
);
db.tournament_player.createIndex({ tid: 1, uid: 1 }, { unique: true });
db.push_device.createIndex({ userId: 1, seenAt: -1 });
db.push_device.createIndex({ seenAt: 1 }, { expireAfterSeconds: 2592000 });
db.plan_charge.createIndex({ userId: 1, date: -1 });
db.plan_charge.createIndex({ date: -1 });
db.plan_charge.createIndex(
  { giftTo: 1, date: -1 },
  { partialFilterExpression: { giftTo: { $exists: true } } },
);
db.f_post.createIndex({ topicId: 1, troll: 1 });
db.f_post.createIndex({ createdAt: -1, troll: 1 });
db.f_post.createIndex({ userId: 1 });
db.f_post.createIndex({ categId: 1, createdAt: -1 }, { background: 1 });
db.f_post.createIndex({ topicId: 1, createdAt: -1 }, { background: 1 });
db.external_engine.createIndex({ userId: 1 });
db.external_engine.createIndex({ oauthToken: 1 });
db.tutor_queue.createIndex({ requestedAt: 1 });
db.clas_student.createIndex({ clasId: 1, userId: 1 });
db.clas_student.createIndex({ userId: 1 });
db.challenge_bulk.createIndex({ pairAt: 1 });
db.challenge_bulk.createIndex(
  { startClocksAt: 1 },
  { partialFilterExpression: { startClocksAt: { $exists: true } } },
);
db.challenge_bulk.createIndex({ by: 1, pairAt: -1 });
db.push_subscription.createIndex({ userId: 1 });
db.team_member.createIndex({ team: 1 });
db.team_member.createIndex({ user: 1 });
db.team_member.createIndex({ team: 1, date: -1 }, { background: 1 });
db.team_member.createIndex({ team: 1, perms: 1 }, { partialFilterExpression: { perms: { $exists: 1 } } });
db.email_domains.createIndex({ nb: -1 }, { background: 1 });
db.game5.createIndex({ ca: -1 });
db.game5.createIndex({ us: 1, ca: -1 });
db.game5.createIndex({ 'pgni.user': 1, 'pgni.ca': -1 }, { sparse: 1 });
db.game5.createIndex({ ck: 1 }, { sparse: 1, background: 1 });
db.game5.createIndex({ pl: 1 }, { sparse: true, background: true });
db.game5.createIndex({ 'pgni.h': 1 }, { sparse: true, background: true });
db.security.createIndex({ user: 1 });
db.security.createIndex({ ip: 1 });
db.security.createIndex({ fp: 1 }, { sparse: 1, background: 1 });
db.study.createIndex({ ownerId: 1, createdAt: -1 });
db.study.createIndex({ likes: 1, createdAt: -1 });
db.study.createIndex({ ownerId: 1, updatedAt: -1 });
db.study.createIndex({ likes: 1, updatedAt: -1 });
db.study.createIndex({ rank: -1 });
db.study.createIndex({ createdAt: -1 });
db.study.createIndex({ updatedAt: -1 });
db.study.createIndex({ likers: 1 });
db.study.createIndex({ uids: 1 }, { background: true });
db.study.createIndex(
  { topics: 1, rank: -1 },
  { partialFilterExpression: { topics: { $exists: 1 } }, background: 1 },
);
db.study.createIndex(
  { topics: 1, createdAt: -1 },
  { partialFilterExpression: { topics: { $exists: 1 } }, background: 1 },
);
db.study.createIndex(
  { topics: 1, updatedAt: -1 },
  { partialFilterExpression: { topics: { $exists: 1 } }, background: 1 },
);
db.study.createIndex(
  { topics: 1, likes: -1 },
  { partialFilterExpression: { topics: { $exists: 1 } }, background: 1 },
);
db.study.createIndex(
  { uids: 1, rank: -1 },
  { partialFilterExpression: { topics: { $exists: 1 } }, background: 1 },
);
db.study_chapter_flat.createIndex({ studyId: 1, order: 1 });
db.study_chapter_flat.createIndex(
  { 'relay.fideIds': 1 },
  { partialFilterExpression: { 'relay.fideIds': { $exists: true } } },
);
db.title_request.createIndex({ userId: 1 });
db.title_request.createIndex({ 'history.0.status.n': 1, 'history.0.at': 1 });

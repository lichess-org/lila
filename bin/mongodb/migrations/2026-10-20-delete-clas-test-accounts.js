// One-off cleanup of the test Chess-Online class accounts found by the prod audit.
// Dry-run by default. To execute: CONFIRM_DELETE=1 mongosh lichess <this file>
//
// Deletes: user4, clas_student, clas_clas/clas_invite of the test class.
// Does NOT delete games/messages/forum posts; orphaned data must be reviewed
// separately (only `greedycrab` has games in the audited dump).

const CONFIRM = process.env.CONFIRM_DELETE === '1';

const USER_IDS = [
  'chess-network',
  'acceptedclam',
  'atanwin',
  'spruceotter',
  'giftedbeekeeper',
  'holygnu',
  'greedycrab',
];
const CLASS_IDS = ['EesFLatG'];

const plan = {
  user4: db.user4.countDocuments({ _id: { $in: USER_IDS } }),
  clas_student: db.clas_student.countDocuments({ userId: { $in: USER_IDS } }),
  clas_clas: db.clas_clas.countDocuments({ _id: { $in: CLASS_IDS } }),
  clas_invite: db.clas_invite.countDocuments({ clasId: { $in: CLASS_IDS } }),
  security: db.security.countDocuments({ user: { $in: USER_IDS } }),
  oauth_access_token: db.oauth_access_token.countDocuments({ userId: { $in: USER_IDS } }),
  oauth2_access_token: db.oauth2_access_token.countDocuments({ userId: { $in: USER_IDS } }),
  push_subscription: db.push_subscription.countDocuments({ userId: { $in: USER_IDS } }),
  appeal: db.appeal.countDocuments({ user: { $in: USER_IDS } }),
  note: db.note.countDocuments({ $or: [{ from: { $in: USER_IDS } }, { to: { $in: USER_IDS } }] }),
  plan_patron: db.plan_patron.countDocuments({ userId: { $in: USER_IDS } }),
  team_member: db.team_member.countDocuments({ user: { $in: USER_IDS } }),
};

printjson({ confirm: CONFIRM, plan });

if (!CONFIRM) {
  print('DRY RUN: set CONFIRM_DELETE=1 to execute');
} else {
  const removed = {
    clas_student: db.clas_student.deleteMany({ userId: { $in: USER_IDS } }).deletedCount,
    clas_clas: db.clas_clas.deleteMany({ _id: { $in: CLASS_IDS } }).deletedCount,
    clas_invite: db.clas_invite.deleteMany({ clasId: { $in: CLASS_IDS } }).deletedCount,
    user4: db.user4.deleteMany({ _id: { $in: USER_IDS } }).deletedCount,
  };
  printjson({ removed });
  print('NOTE: games/messages/forum data for these ids were not touched');
}

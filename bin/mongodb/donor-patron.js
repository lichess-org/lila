var now = new Date();
var inOneMonth = new Date(ISODate().getTime() + 1000 * 60 * 60 * 24 * 31);

db.plan_patron.remove({});
db.user4.update({plan:{$exists:true}},{$unset:{plan:true}},{multi:true});

var userIds = [];
function donationToPatron(donation) {
  if (userIds.indexOf(donation.userId) !== -1) return;
  userIds.push(donation.userId);
  var patron = {
    _id: donation.userId,
    payPal: {
      lastCharge: donation.date
    },
    lastLevelUp: now,
    expiresAt: inOneMonth
  }
  if (donation.payPalSub) patron.payPal.subId = donation.payPalSub;
  if (donation.email) patron.payPal.email = donation.email;
  db.plan_patron.insert(patron);
  db.user4.update({
    _id: donation.userId
  }, {
    $set: {
      plan: {
        active: true,
        months: NumberInt(1)
      }
    }
  });
}

db.donation.find({userId:{'$exists':true}}).sort({date:-1}).forEach(donationToPatron);

donationToPatron({
  userId: 'thibault',
  date: new Date(ISODate().getTime() - 1000 * 60 * 60 * 24 * 31 * 3),
  email: 'lichess.contact@gmail.com'
});

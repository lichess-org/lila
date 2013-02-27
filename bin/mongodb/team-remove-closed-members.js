print("Removing closed users memberships");

db.team_member.find().forEach(function(member) {
  var user = db.user2.findOne({_id: member.user});
  if (!user || !user.enabled) {
    print("Removing " + member._id);
    db.team_member.remove({_id: member._id});
  }
});

print("Done!");

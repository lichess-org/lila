db.game5.find({so:7, 'pgni.user': {$exists: true}}).forEach(function(g) {
  delete g.ua;
  delete g.tv;
  g.pgni.ca = g.ca;
  db.game5.update({_id:g._id},{$set:{'pgni.ca': g.ca},$unset:{ua:true,tv:true}});
});

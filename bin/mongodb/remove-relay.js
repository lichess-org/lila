var id = 'RWtJ4aKC';

var relay = db.relay.findOne({_id:id});
if (relay) {
  print('Removing ' + relay.games.length + ' games');
  relay.games.forEach(function(g) {
    db.game5.remove({_id:g.id});
  });
  print('Removing relay');
  db.relay.remove({_id:id});
}

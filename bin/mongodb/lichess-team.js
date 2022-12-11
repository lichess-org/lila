fromId = 'lic-king-of-the-hill';
id = 'lichess-king-of-the-hill';
name = 'Lichess King of the Hill';

team = db.team.findOne({ _id: fromId });

team._id = id;
team.name = name;

db.team.insert(team);
db.team.remove({ _id: fromId });

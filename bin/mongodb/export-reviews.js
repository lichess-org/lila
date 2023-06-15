const text = () =>
  aggregate().forEach(coach => {
    print();
    print('===================================');
    print('https://lichess.org/coach/' + coach._id);
    print('===================================');
    print();
    coach.reviews.forEach(review => {
      print(review.score + '/5' + ' by https://lichess.org/' + review.from);
      print('       ' + review.text);
      print('-----------------------------------');
    });
  });

const json = () => printjson(aggregate().toArray());

const aggregate = () =>
  db.coach_review.aggregate([
    { $group: { _id: '$coachId', reviews: { $push: { from: '$userId', text: '$text', score: '$score' } } } },
  ]);

json();

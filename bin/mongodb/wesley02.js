db.tournament2.insert({
  _id: 'wesley02',
  clock: {
    limit: NumberInt(3 * 60),
    increment: NumberInt(0),
  },
  createdAt: new Date(),
  createdBy: 'lichess',
  minutes: NumberInt(60),
  name: 'GM Wesley So Arena',
  nbPlayers: NumberInt(0),
  startsAt: ISODate('2016-03-02T22:00:00Z'),
  schedule: {
    freq: 'unique',
    speed: 'blitz',
  },
  status: NumberInt(10),
  variant: NumberInt(1),
  spotlight: {
    homepageHours: 24,
    headline: 'Tournament by Chess at Three',
    description:
      'Your chance to play Super GM Wesley So! Tournament organized and [livestreamed](www.twitch.tv/chessat3) by [chessat3.com](http://chessat3.com).',
    iconImg: 'chessat3.logo.png',
  },
});

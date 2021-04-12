if (typeof user == 'undefined') throw 'Usage: mongo lichess --eval \'user="username"\' script.js';

user = db.user4.findOne({ _id: user });

if (!user || user.enabled || !user.erasedAt) throw 'Erase with lichess CLI first.';

const randomUserId = () => {
  const idChars = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
  const idLength = 8;
  let result = '';
  for (let i = idLength; i > 0; --i) result += idChars[Math.floor(Math.random() * idChars.length)];
  return result;
};

const id = randomUserId();

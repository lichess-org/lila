const tid = 'a/b';
const renderDate = date => `${date.getDate()}/${date.getMonth() + 1}/${date.getFullYear()}`;

db.msg_msg
  .find({ tid: tid })
  .sort({ date: 1 })
  .forEach(m => {
    print(`${renderDate(m.date)} ${m.user}`);
    print(m.text);
    print('');
    print('-----------------------------------------------------------------------------');
    print('');
  });

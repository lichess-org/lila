db.user2.update(
  {},
  {
    $unset: {
      'settings.chat': 1,
      'settings.sound': 1,
    },
  },
  { multi: 1 },
);

// https://github.com/lichess-org/lila/pull/16764

function plusNormalize(email) {
  let [name, domain] = email.toLowerCase().split('@');
  [name] = name.split('+');
  return name + '@' + domain;
}

db.user4.find({ email: /^[^+]+\+.*@.+$/i }).forEach(user => {
  const normalized = plusNormalize(user.email);
  const verbatim = user.verbatimEmail || user.email;
  print(user.username, ': ', verbatim, '->', normalized);

  db.user4.update(
    {
      _id: user._id,
    },
    {
      $set: {
        email: normalized,
        verbatimEmail: verbatim,
      },
    },
  );
});

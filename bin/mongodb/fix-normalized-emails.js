function gmailNormalize(email) {
  let [name, domain] = email.toLowerCase().split('@');
  [name] = name.split('+');
  return name.replace(/\./g, '') + '@' + domain;
}

db.user4
  .find({ email: /[^+.]+[+.].*@(protonmail\.com|protonmail\.ch|pm\.me|gmail\.com|googlemail\.com)$/i })
  .forEach(user => {
    const normalized = gmailNormalize(user.email);
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
      }
    );
  });

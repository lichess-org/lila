const gmailOrProton = ['protonmail.com', 'protonmail.ch', 'pm.me', 'gmail.com', 'googlemail.com'];

function normalize(email) {
  let [name, domain] = email.toLowerCase().split('@');
  [name] = name.split('+');

  if (gmailOrProton.includes(domain)) {
    return name.replace(/\./g, '') + '@' + domain;
  } else {
    return name + '@' + domain;
  }
}

db.user4
  .find({
    email:
      /([^+.]+[+.].*@(protonmail\.com|protonmail\.ch|pm\.me|gmail\.com|googlemail\.com)|^[^+]+\+.*@.+)$/i,
  })
  .forEach(user => {
    const normalized = normalize(user.email);
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

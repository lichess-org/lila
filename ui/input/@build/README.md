# usage:

```
node dist/makeLexicon.js --freq=.004 --count=5 --max-ops=1 --in=feb26.json

The idea is that no transform on a heard phrase should be allowed if the sum of its
substitution costs exceed some value, say 1.0.

To determine costs with makeLexicon:

  --freq is minimum frequency of a substitution to be considered
  --count is minimum occurrences of a substitution in the input data
  --max-ops is the maximum number of substitutions to transform a heard phrase to an exact one.

  if --max-opts=1 (default), a substitution is only valid if it alone corrects at least
    --count phrases in the input data with a frequency above --freq
  For --max-opts=n, at most n substitutions must correct --count phrases with frequency
    above --freq

TODO: explain more
```

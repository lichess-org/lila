# usage:

```
node dist/makeLexicon.js --freq=.004 --count=5 --max-ops=1 --in=feb26.json


  --freq is minimum frequency of a substitution to be considered
  --count is minimum occurrences of a substitution in the input data
  --max-ops is the maximum number of substitutions to transform a heard phrase to an exact one.

The idea, base on rigorous ass math, is that no transform on a heard phrase should
be allowed if the sum of its substitution costs exceed 1.0.

To determine costs (the purpose of this script):

  For default --max-opts=1, a substitution is only valid if it alone corrects at least
    --count phrases in the input data with a frequency above --freq

  For --max-opts=2, a substitution is only valid if it and at most 1 other substitution
    corrects --count phrases with frequency above --freq


TODO: explain all this shit
```

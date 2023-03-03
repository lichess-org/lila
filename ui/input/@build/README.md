# usage:

```
node dist/makeGrammar.js --freq=.004 \ # default values given explicitly here
                         --count=6 \
                         --max-ops=1 \
                         --in=./crowdv-27-02-2023.json \
                         --out=../src/voiceMoveGrammar.ts

the idea is that no transform on a heard phrase should be allowed if the sum of its
substitution costs exceed some value, say 1.0. the makeGrammar script generates a lexicon
of kaldi words together with token values and substitution rules at src/voiceMoveGrammar.ts
we'll commit that to git even tho it's generated. we should not need to run makeGrammar very
frequently once the dust settles

arguments:

  --freq is minimum frequency of a substitution to be considered
  --count is minimum occurrences of a substitution in the input data
  --max-ops is the maximum allowed distance (number of substitution ops) to transform a
            heard phrase to an exact one.

  if {max-ops} of 1 (default), a substitution is only valid if it alone corrects at least
    {count} phrases in the input data with a frequency above {freq}
  For {max-ops} of n, at most n substitutions must correct {count} phrases with frequency
    above {freq}

TODO: explain more
```

# overview:

```
the idea is that no transform on a heard phrase should be allowed if the sum of its
substitution costs exceed some value, say 1.0. the makeGrammar script generates a lexicon
of kaldi words together with token values and substitution rules at src/voiceMoveGrammar.ts
we'll commit that to git even tho it's generated. we should not need to run makeGrammar very
frequently once the dust settles
```

# usage:

```
node dist/makeGrammar.js --freq=.003 \ # default values given explicitly here
                         --count=6 \
                         --max-ops=1 \
                         --in=./crowdv-27-02-2023.json \
                         --out=../src/voiceMoveGrammar.ts

arguments:
  {in} is a locally resolved path or a https url to a crowdv json, do not use this
    argument unless you want to operate on custom data sets

  {out} is a relative path to the generated grammar file. specify this if you want to
    generate grammars at a non-standard location

  {freq} is the minimum frequency of a valid substitution. frequency is calculated as the
    number of input phrases corrected by the substitution divided by the total number of
    occurrences of the token the substitution operates on.

  {count} is minimum occurrences of a substitution in the input data

  {max-ops} is the maximum allowed distance (number of substitution ops) to transform a
    heard phrase to an exact one. if {max-ops} is 1 (default), a substitution is only valid
    if it alone corrects at least {count} input phrases at a frequency above {freq}.
    for {max-ops} of n, at most n substitutions must correct {count} phrases with frequency
    above {freq}

see the source code for further customization

TODO: explain more
```

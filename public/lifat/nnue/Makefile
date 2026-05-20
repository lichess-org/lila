SOURCES := $(wildcard *.nnue)
TARGETS := $(SOURCES:.nnue=.nnue.br) $(SOURCES:.nnue=.nnue.gz)

all: $(TARGETS)

%.br: %
	brotli $< --keep --stdout > $@.tmp
	mv $@.tmp $@

%.gz: %
	gzip $< --keep --stdout > $@.tmp
	mv $@.tmp $@

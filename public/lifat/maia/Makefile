SOURCES := $(wildcard *.pb)
TARGETS := $(SOURCES:.pb=.pb.br) $(SOURCES:.pb=.pb.gz)

all: $(TARGETS)

%.br: %
	brotli $< --keep --stdout > $@.tmp
	mv $@.tmp $@

%.gz: %
	gzip -9 --no-name $< --keep --stdout > $@.tmp
	mv $@.tmp $@

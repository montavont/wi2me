# You want latexmk to *always* run, because make does not have all the info.
# Also, include non-file targets in .PHONY so they are run regardless of any
# file of the given name existing.
.PHONY: MyDoc.pdf all clean

# The first rule in a Makefile is the one executed by default ("make"). It
# should always be the "all" rule, so that "make" and "make all" are identical.
all: wi2meU_userdoc_en wi2meU_userdoc_fr

#
# CUSTOM BUILD RULES
#
# In case you didn't know, '$@' is a variable holding the name of the target,
# and '$<' is a variable holding the (first) dependency of a rule.
# "raw2tex" and "dat2tex" are just placeholders for whatever custom steps
# you might have.
#
%.tex: %.raw
	./raw2tex $< > $@

%.tex: %.dat
	./dat2tex $< > $@

figures: start_screen stop_screen upload_screen

%_screen:
	inkscape --export-png=figures/$@.png figures/$@.svg

wi2meU_userdoc_en: wi2meU_userdoc.tex figures
	pdflatex -interactive=nonstopmode -jobname=$@ $<

wi2meU_userdoc_fr: wi2meU_userdoc.tex figures
	pdflatex -interactive=nonstopmode -jobname=$@ "\def\frenchmanual{}\input{$<}"

# Short rules
fr : wi2meU_userdoc_fr

en : wi2meU_userdoc_en

clean:
	latexmk -CA

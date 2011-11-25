#
# 	Makefile for latex
#

#	targets:
#	 <default>	-- build document

# =============================================================================

MAIN_DE = main
MAIN_EN = main_en
MANUAL_DE = manual-de
MANUAL_EN = manual-en

PDFLATEX = pdflatex

all: $(MANUAL_DE) $(MANUAL_EN)

$(MANUAL_DE): $(MAIN_DE).tex
	$(PDFLATEX) --jobname $(MANUAL_DE) $(MAIN_DE)

$(MANUAL_EN): $(MAIN_EN).tex
	$(PDFLATEX) --jobname $(MANUAL_EN) $(MAIN_EN)

clean:
	rm -f $(MANUAL_DE).aux $(MANUAL_DE).log $(MANUAL_DE).toc $(MANUAL_DE).out
	rm -f $(MANUAL_EN).aux $(MANUAL_EN).log $(MANUAL_EN).toc $(MANUAL_EN).out

tidy: clean
	rm -f $(MANUAL_DE).pdf $(MANUAL_EN).pdf

### end ###

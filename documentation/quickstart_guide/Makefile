MAIN_EN=main_en
MAIN_DE=main_de
QUICK_EN=quickstart-en
QUICK_DE=quickstart-de
PDFLATEX = pdflatex

all: $(QUICK_EN) $(QUICK_DE)

$(QUICK_EN): $(MAIN_EN).tex
	$(PDFLATEX) --jobname $(QUICK_EN) $(MAIN_EN)

$(QUICK_DE): $(MAIN_DE).tex
	$(PDFLATEX) --jobname $(QUICK_DE) $(MAIN_DE)

clean:
	rm -f $(QUICK_EN).aux $(QUICK_EN).log $(QUICK_EN).toc $(QUICK_EN).out
	rm -f $(QUICK_DE).aux $(QUICK_DE).log $(QUICK_DE).toc $(QUICK_DE).out

tidy:
	rm -f $(QUICK_EN).pdf
	rm -f $(QUICK_DE).pdf

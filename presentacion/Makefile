# Makefile for Structured Concurrency presentation

MAIN = structured-concurrency-presentation
LATEX = pdflatex
BIBTEX = bibtex
LATEX_FLAGS = -shell-escape -interaction=nonstopmode

$(MAIN).pdf: $(MAIN).tex
	$(LATEX) $(LATEX_FLAGS) $(MAIN).tex
	$(BIBTEX) $(MAIN)
	$(LATEX) $(LATEX_FLAGS) $(MAIN).tex
	$(LATEX) $(LATEX_FLAGS) $(MAIN).tex

quick: $(MAIN).tex
	$(LATEX) $(LATEX_FLAGS) $(MAIN).tex

clean:
	rm -f *.aux *.log *.nav *.out *.snm *.toc *.vrb *.fls *.fdb_latexmk
	rm -f *.bbl *.blg *.bcf *.run.xml
	rm -rf _minted-$(MAIN)/

cleanall: clean
	rm -f $(MAIN).pdf

.PHONY: quick clean cleanall

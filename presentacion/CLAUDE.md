# Presentation Validation Workflow

## Typical Workflow
- User runs `continuous-build.sh` in the background for auto-rebuild on .tex changes
- Claude makes edits to .tex files
- Claude validates the visual result (see below)

## Validating Visual Results

After making changes to .tex files, validate the result by converting PDF pages to images:

```bash
# Get total page count
pdfinfo presentacion/structured-concurrency-presentation.pdf | grep Pages

# Convert specific page to PNG (e.g., page 2 for introduction)
pdftoppm -png -f 2 -l 2 -r 150 presentacion/structured-concurrency-presentation.pdf presentacion/page
```

This creates `presentacion/page-2.png` which can be read to visually inspect the result.

## Build Commands (Reference)

### Quick Build
```bash
cd presentacion && make quick
```

### Full Build with Bibliography
```bash
cd presentacion && make
```

### Clean
```bash
cd presentacion && make clean      # Remove auxiliary files
cd presentacion && make cleanall   # Remove all files including PDF
```

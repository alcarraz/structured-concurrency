# Makefile para generar diagramas PlantUML

# Directorio de diagramas
DOCS_DIR = .

# Archivos fuente PlantUML
PUML_FILES = $(wildcard $(DOCS_DIR)/*.puml)

# Archivos PNG de salida
PNG_FILES = $(PUML_FILES:.puml=.png)

# Target por defecto: generar todos los PNG
.PHONY: all
all: $(PNG_FILES)

# Regla para generar PNG desde PUML
%.png: %.puml
	@echo "Generando $@..."
	plantuml $<

# Generar solo diagramas principales (usados en README)
.PHONY: main
main: diagrama-models.png diagrama-servicios.png diagrama-procesadores.png

# Generar diagramas individuales de procesadores
.PHONY: processors
processors: diagrama-reactive.png diagrama-structured.png diagrama-scoped.png

# Limpiar archivos PNG generados
.PHONY: clean
clean:
	@echo "Eliminando archivos PNG generados..."
	rm -f $(DOCS_DIR)/diagrama-*.png

# Reconstruir todo el proyecto (limpiar y generar)
.PHONY: rebuild
rebuild: clean all

# Mostrar ayuda
.PHONY: help
help:
	@echo "Makefile para generar diagramas PlantUML"
	@echo ""
	@echo "Targets disponibles:"
	@echo "  all        - Generar todos los diagramas PNG (default)"
	@echo "  main       - Generar solo diagramas principales (models, servicios, procesadores)"
	@echo "  processors - Generar solo diagramas individuales de procesadores"
	@echo "  clean      - Eliminar archivos PNG generados"
	@echo "  rebuild    - Limpiar y regenerar todos los diagramas"
	@echo "  help       - Mostrar esta ayuda"
	@echo ""
	@echo "Diagramas principales (usados en README):"
	@echo "  - diagrama-models.png"
	@echo "  - diagrama-servicios.png"
	@echo "  - diagrama-procesadores.png"
	@echo ""
	@echo "Diagramas de referencia (no en README):"
	@echo "  - diagrama-reactive.png"
	@echo "  - diagrama-structured.png"
	@echo "  - diagrama-scoped.png"

SHELL=/bin/sh

all :: test rng validate

test :: ../rng/pml_schema.rng
	for s in *_schema.xml; do \
	  ../tools/rng_validate ../rng/pml_schema.rng "$$s"; \
	done

rng :: ../tools/pml2rng.xsl
	for s in *_schema.xml; do \
	  ../bin/pml_simplify "$$s" \
	  | xsltproc ../tools/pml2rng.xsl - \
	  |xmllint --format - > "$${s%.xml}.rng"; \
	done

validate :: ../pml2rng.xsl
	for ex in example*[0-9].xml; do \
	   ../tools/rng_validate "$${ex%.xml}_schema.rng" $${ex};\
	done

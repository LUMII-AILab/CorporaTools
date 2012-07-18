<?xml version="1.0" encoding="utf-8"?>
<pmlbackend
  xmlns="http://ufal.mff.cuni.cz/pdt/pml/"
  xmlns:xi="http://www.w3.org/2001/XInclude">
  <head>
    <schema href="pmlbackend_conf_schema.xml"/>
  </head>
  <transform_map>
    <transform id="pmL_schema_tree" test="*[local-name()='pml_schema' and namespace-uri()='http://ufal.mff.cuni.cz/pdt/pml/schema/']">
      <in type="xslt" href="pmlschema2pml.xsl"/>
    </transform>
  </transform_map>
</pmlbackend>

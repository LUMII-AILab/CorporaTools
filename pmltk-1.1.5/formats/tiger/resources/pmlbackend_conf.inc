<?xml version="1.0"?>
<pmlbackend
  xmlns="http://ufal.mff.cuni.cz/pdt/pml/">
  <head>
    <schema href="pmlbackend_conf_schema.xml"/>
  </head>
  <transform_map>
    <!-- 
      Conversion from Tiger XML format.
      *
      * Note: may run out of memory for HUGE xml files !!!
      *
    -->
    <transform id="tigerXML" test="/corpus/head/annotation">
      <in type="xslt" href="tiger2pml.xsl"/>
      <!-- NOT YET DEFINED: -->
      <!-- <out type="xslt" href="pml2tiger.xsl"/> -->
    </transform> 
  </transform_map>
</pmlbackend>

# ncl_workflow-engine
## Introduction
Distributed workflow engine for workflow scheduling and service function chaining **(SFC)** for real environment.
One function corresponds to one linux commnad or a Docker container.
Though there are several workflow enginge to handle task allocation, this workflow engine, i.e., **ncl_wokflow-engine(nclw)** is characterized to have a specific task/function scheduling algorithm, called **S**ervice **F**unction **C**lustering for **U**tilizing **v**CPUs (**SF-CUV**) algorithm. In particular, nclw includes other well-known function scheduling algorithms such as HEFT, PEFT, CoordVNF, and FWS. 

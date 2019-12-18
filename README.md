# ncl_workflow-engine(NCLW)
## Introduction
Distributed workflow engine for workflow scheduling and service function chaining **(SFC)** for a real environment.
One function corresponds to one linux commnad or a Docker container.
Though there are several workflow enginges to handle task allocation, this workflow engine, i.e., **ncl_wokflow-engine(NCLW)** is characterized to have a specific task/function scheduling algorithm, called **S**ervice **F**unction **C**lustering for **U**tilizing **v**CPUs (**SF-CUV**) algorithm. In particular, NCLW includes other well-known function scheduling algorithms such as HEFT, PEFT, CoordVNF, and FWS. 
## 機能
- ワークフロー型ジョブをサービスファンクションチェイン（SFC）と見立てて，各ファンクションのスケジューリングを行います．
  スケジューリングアルゴリズムとしてはSF-CUV, HEFT, PEFT, FWS, CoordVNFを実装しており，設定ファイルにて選択できます．
- チェイニング方式: IPベースによるチェイニングに加えて，ICNによるチェイニングが可能です．ICNによって一度実行したファンクションの結果データを，再実行することなく取得でき，処理時間の節約になります．

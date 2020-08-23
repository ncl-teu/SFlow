Readme(japanese) is [here](https://github.com/ncl-teu/SFlow/blob/master/README_JP.md): 
# SFlow: Workflow-engine for IP/ICN based service function chaining with scheduling algorithms. 
## Summary of SFlow
- SFlow supports task/function scheduling in a workflow by allocating each function to a vCPU/CPU core for each physical CPU. 
- Several task scheduling algorithms are included in SFlow, and each of them can be set by the configuration file. 
- A function is a docker container in SFlow. When a function is ready for execution, SFlow check whether the batch program is installed or not on the node. If so, the function is processed by the system call, otherwise, it attempts to pull the docker image from the docker repository and load it before the execution. 
- Chaining scheme: IP-based chaining and ICN-based chaining are supported in SFlow. If ICN-based chaining is enabled, the resultant data is cached on the node, thereby the cache is returned to the successor functions if the interest packet is arrived without execution. 
## System structure
An assumed system structure is shown the figure. 
- **Delegator**: Schedule each function using Workflow information(JSON file) and node informatino(JSON file). 
- **File server**: It provides input files as necessary through FTP and SSH protocols. 
- **Docker repository(private)**: It has dockar images, and it is pulled from nodes as necessary. (if no docker container is loaded on the node). 
- **ワーカ群**:  The sef of VMs and physical nodes to which IP addresses are allocateds. 

![system](https://user-images.githubusercontent.com/4952618/73509139-7a055200-4421-11ea-9108-245a240a87be.png)

## セットアップ
### 設定ファイル（Delegator + ワーカに配備）
- 設定ファイルは**nclw2.properties**で，下記のように設定します．自身で設定する必要がある箇所のみ表示しています．
使用時には，このリポジトリにあるnclw2.propertiesを用いて，書き換えてください（↓をコピペしないでください）．
~~~
# IPベースによる通信ポート番号．例: 8088
port_number=
# DelegatorのIPアドレス．例: 192.168.1.17
delegator_ip=

# Delegatorで行うSFCのスケジューリングアルゴリズム．下記の番号を指定してください．
# 0: SF-CUV 1: HEFT 2: FWS 3: CoordVNF 4: HClustering 5: PEFT
sched_algorithm=

# 実行に必要な入力ファイルの転送プロトコル．現状は"ftp"を指定してください．
input_file_transfer_protocol=ftp

# FTPサーバのIPアドレスか，ホスト名を指定してください．
ftp_server_ip=

# FTPサーバへのログインID
ftp_server_id=

# FTPサーバへのログインパスワード
ftp_server_pass=

# FTPサーバの絶対パスのホームディレクトリ．最後は"/"をつけないでください．例: /home/test/workflow
ftp_server_homedirName=nclw

# DockerリポジトリのIPアドレス．
docker_repository_ip=

# Dockerリポジトリのイメージファイルが格納されているパス．最後は"/"をつけないでください．例: /home/test/images
docker_repository_home=

# DockerリポジトリのSSHログインのユーザID
docker_repository_userid=

# DockerリポジトリのSSHログインのパスワード
docker_repository_password=

# ワーカのワーキングディレクトリにおいて，Dockerイメージファイル(.tar）を格納するディレクトリ
# 例: ワーカのワーキングディレクトリ: /home/test/work/ で，docker_localdir=docker_tar とすると，Docker
# リポジトリからSSH経由で取得したtarファイルは，
# /home/test/work/docker_tar 以下に保存される．
docker_localdir=docker_tar
~~~ 
### 環境情報のファイル(Delegatorで保持）
- 環境情報ファイル(JSON)の形式は，↓のとおりです．実際に使われる特定環境用のファイルは，nclw/env_ncl.json に記載されています．
- 下記の例は，データセンター(クラウド)が一つで，物理ホストがデータセンターに1台あります．当該ホストは2コアで，各コアではHyper-ThreadがONであるため，
   1コアあたり2つのvCPU（仮想CPU）が動作可能ということです．
- また，当該ホストは2つのVMが稼働しており，1つ目のVMは2つのvCPU，VM_2は1つのvCPUにマッピングされている状態です．
- この場合は，vCPU番号0^0^0^1^0はアイドル状態となっており，それ以外はVM上の処理に使われています．
~~~
{
  "dc_list": [
    {
      "dc_id": 0, "bw": 125, "host_list": [
      { "host_id": "0^0", "name": "ncl-cloud", "ip": "192.168.1.xx", "ram": 128000000, "bw": 125, "cpu_list": [
        { "cpu_id": "0^0^0", "mips": 3000, "core_list": [
          { "core_id": "0^0^0^0", "mips": 3000, "maxusage": 100, "vcpu_list": [
            { "vcpu_id": "0^0^0^0^0", "mips": 3000 },
            { "vcpu_id": "0^0^0^0^1", "mips": 3000 }
          ]
          },
          {  "core_id": "0^0^0^1", "mips": 3000,  "maxusage": 100, "vcpu_list": [
            { "vcpu_id": "0^0^0^1^0", "mips": 2500 },
            { "vcpu_id": "0^0^0^1^1", "mips": 2500 }
          ]
          }
        ]
        }
      ],
      "vm_list": [
          { "vm_id": "0^0^0", "ram": 8096, "ip": "192.168.1.xxx", "vcpu_list": [
            { "vcpu_id": "0^0^0^0^0"},
            { "vcpu_id": "0^0^0^0^1" }
          ]
          },
            {"vm_id": "0^0^1", "ram": 8096, "ip": "192.168.1.xxx", "vcpu_list": [
            { "vcpu_id": "0^0^0^1^0" }
          ]
          }
        ]
      }
    ]
    }
  ]
}
~~~
### Workflowジョブの設定（Delegatorで保持）
- Job情報のファイル(JSON)は，↓のとおりです．nclw/job_ncl.json またはnclw/job_ffmpeg.jsonに記載されています．
- 下記のJobは，nclw/ffmpeg.jsonのフォーマットです．
- $R^ファイル　は，もし無ければFTPサーバからダウンロードしてくるという意味です．
- $F^先行ファンクションIDは，先行ファンクションからの出力データを入力とする，という意味です．
- 後続ファンクションリストはdest_task_idにて複数指定します．そして，dest_taskに相当するファンクションにて，$F^先行ファンクションIDと指定します．
- DUMMYは，Dockerのentrypointに引き渡す場合は無視されます．実際には2つ目移行の箇所がentrypointへ渡されます．
- rootは，ジョブIDで，その中にtask_listがそれぞれ格納されています．
~~~
{
  "job_id": 1,
  "task_list": [
    {
      "task_id": 1, "workload": 10000, "type": 1, "usage": 70, "cmd": "ffmpeg -i  $R^./ncloutput/input.mp4 ./ncloutput/1st.flv",
      "out_file_list": [
      { "path": "./ncloutput/1st.flv", "size": 5,"dest_task_id": [2,3 ] }],
      "docker-tarname":"ffmpeg_1st.tar", "docker-imagename": "ffmpeg_1st"
    },
    {
      "task_id": 2, "workload": 100000, "type": 2, "usage": 80, "cmd": "DUMMY -i  $F^1 -r 60 ./ncloutput/image_of_2nd_%03d.png ./ncloutput/ffmpeg_2nd.zip ./ncloutput/*.png",
      "out_file_list": [
        {"path": "./ncloutput/ffmpeg_2nd.zip", "size": 150, "dest_task_id": [4] }
      ],
      "docker-tarname":"ffmpeg_2nd.tar", "docker-imagename": "ffmpeg_2nd"
    },
    {
      "task_id": 3, "workload": 6000, "type": 3, "usage": 40, "cmd": "ffmpeg -i $F^1 ./ncloutput/3rd.gif",
      "out_file_list": [
        {"path": "./ncloutput/3rd.gif", "size": 68, "dest_task_id": [4] }
      ],
      "docker-tarname":"ffmpeg_3rd.tar", "docker-imagename": "ffmpeg_3rd"
    },
    {
      "task_id": 4, "workload": 80000, "type": 4, "usage": 60, "cmd": "DUMMY  ./ncloutput/4th.mp4 ./ncloutput/ffmpeg_2nd.zip -i ./ncloutput/image_of_2nd_%03d.png ./ncloutput/4th.mp4",
      "out_file_list": [
        {"path": "./ncloutput/4th.mp4", "size": 5, "dest_task_id": [] }
      ],
      "docker-tarname":"ffmpeg_4th.tar", "docker-imagename": "ffmpeg_4th"
    }
  ]}
~~~
- 例えばtask_idが2の場合は，DockerFileは
~~~
FROM alpine:latest
WORKDIR /
RUN apk --update add ffmpeg && \
    mkdir mnt_point && apk --update add zip
COPY docker-entrypoint.sh ./
ENTRYPOINT ["./docker-entrypoint.sh"]
~~~
となっており，docker-entrypoint.shは，
~~~
#!/bin/sh

ffmpeg $1 $2 $3 $4 $5
zip $6 $7
~~~
という形式になっています．そしてdocker-entrypoint.shに対して，先程のJSONファイルのDUMMY移行の引数が入ってくる仕組みです．
### Dockerイメージの作成と，リポジトリへの配備
- DockerFileとdocker-entryopoint.shのあるディレクトリにて`docker save イメージ名 -o 名前.tar` にて，イメージファイル(.tar）を作成します．
- あとは，Dockerリポジトリの指定場所（docker_repository_home）にtarファイルをアップロードしておけばOKです．イメージ名，名前ともにdocker-imagenameの値です．

## 使い方
### 0. スケジューリング結果のJsonファイル化
`./schedgen.sh [環境情報ファイル] [ジョブ情報ファイル] [Jsonファイルの出力先パス]`という形式です．たとえば，
`./schedgen.sh nclw/env.json job_ffmpeg.json ret.json`とすれば，nclw/env.jsonの環境情報，nclw_job_ffmpeg.jsonのワークフロージョブ情報を読み込んでスケジューリングをして，そしてret.jsonにファイルが新規作成（もしくは上書き）されます．出力先の構造ですが，**ジョブ情報ファイルの各ファンクション(task_list内の各taskノード）に，割当先のipアドレス(ip)，vCPUID(vcpu_id)，VMのID(vm_id)が付与された状態になっています**. つまり，割当先の性能を知りたければ，環境情報ファイルを別途読み込んでおくことが必要になります．
### 1. IP-based SFC
#### 1.1 起動
1. Delegator側にて，`./nclw_startworkder.sh`によって**nclw_hosts**に記載されている全ワーカー側プロセスを起動．これにより，各ワーカーは，データ受信待ちをする．
2. Delegator側にて，`./nclw_delegator.sh`によってスケジューリングを行い，ジョブ情報と環境情報をENDファンクション処理ノードへ送信される．そして，スケジュール通りに処理＋通信がなされる．
3. ワーカープロセスを終了させる場合は，Delegator側にて，`./nclw_stopworker.sh`によって一斉終了させる．

### 2. ICN-based static SFC
#### 2.1 起動
1. Delegator側にて，`./nclw_nfdstartworkder.sh`によって**nclw_hosts**に記載されている全ワーカー側プロセスを起動．これにより，各ワーカーは，データ受信待ちをする．
2. Delegator側にて，`./nclw_nfdelegator.sh`によってスケジューリングを行い，ジョブ情報と環境情報をENDファンクション処理ノードへ送信される．そして，スケジュール通りに処理＋通信がなされる．
3. ワーカープロセスを終了させる場合は，Delegator側にて，`./nclw_stopworker.sh`によって一斉終了させる．
4. 実行ログを見る場合は，Delegator側にて`./nclw_cfdcollectlog.sh`で**nclw_hosts**に記載されている全ワーカー側のログを収集して，collectLog@Delegatorに書き込まれます．その後，**collectLog**を見て実行ログを確認してください．また，各ワーカーでの単独ログは，ワーカーのnclwLogに書き込まれていますので，個別に確認する場合はnclwLog@ワーカーを見てください．

### 3. ICN-based autonomous SFC(AutoICN-SFC)
各ノードが，自身のFIBの中からSFの割当先を決めて，その結果に基づいてICN-SFCを行うアルゴリズム実装です．

### APIについて
#### Faceの作成
`TcpFace Face名 = NclwNFDMgr.getIns().createFace(宛先IPアドレス, 送信元IPアドレス);`
#### FIBへのFace追加
`NclwNFDMgr.getIns().getFib().insert(Name型のPrefix, 追加するFace, コスト値(例: 1）);`
#### PITへのFace追加
`NclwNFDMgr.getIns().getPit().insert(Interest型のinterest);`

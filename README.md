# SFlow: Workflow-engine for multiple communication schemes with scheduling algorithms. 
## SFlowの概要
- サービスファンクションのワークフロージョブにおいて，VM内の仮想CPU(vCPU)，または物理ホストの各コアに対して，各ファンクションのスケジューリングを行って割り当てます．つまりネットワーク上に分散した仮想／物理ホストに対してサービスファンクション配備するワークフローエンジンです．
  - スケジューリングアルゴリズムとしてはSF-CUV, HEFT, PEFT, FWS, CoordVNFを実装しており，設定ファイルにて選択できます．
- ファンクション=Dockerコンテナであり，まずはホスト内にバッチ処理がインストールされているかチェックします．インストール済みであればそのまま実行し，されていなければ指定のDockerリポジトリからDockerイメージをロードしてから実行します．
- チェイニング方式: IPベースによるチェイニングに加えて，ICNによるチェイニングが可能です．ICNによって一度実行したファンクションの結果データを再実行することなく取得でき，処理時間の節約になります．
## 構成
想定する環境ですが，↓の図のように，
- **Delegator**: ワークフロー情報(JSONファイル），ワーカー情報（JSONファイル），ジョブ情報（JSONファイル）を保持して，スケジューリングする．
- **ファイルサーバ**: ジョブ実行に必要なファイルを保持し，FTP経由で提供する
- **Dockerリポジトリ**: Dockerイメージを格納しておき，実行時にワーカーへ提供する
- **ワーカ群**: VM，物理ホスト集合であり，ファンクション実行を行うノード集合．IPアドレスが割り当てられたもの

が必要です．
![system](https://user-images.githubusercontent.com/4952618/73509139-7a055200-4421-11ea-9108-245a240a87be.png)

## Delegator側の設定
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
# ftp_server_ipと同じ値を指定してください．
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
            {"vm_id": "0^0^12", "ram": 8096, "ip": "192.168.1.xxx", "vcpu_list": [
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
## 使い方
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

### 3. ICN-based autonomous SFC

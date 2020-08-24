README(Japanese) is [here](https://github.com/ncl-teu/SFlow/blob/master/README_JP.md): 
# SFlow: Workflow-engine for IP/ICN based service function chaining with scheduling algorithms. 
## Summary of SFlow
- SFlow supports task/function scheduling in a workflow by allocating each function to a vCPU/CPU core for each physical CPU. 
- Several task scheduling algorithms are included in SFlow, and each of them can be set by the configuration file. 
- A function is a docker container in SFlow. When a function is ready for execution, SFlow check whether the batch program is installed or not on the node. If so, the function is processed by the system call, otherwise, it attempts to pull the docker image from the docker repository and load it before the execution. 
- Chaining scheme: IP-based chaining and ICN-based chaining are supported in SFlow. If ICN-based chaining is enabled, the resultant data is cached on the node, thereby the cache is returned to the successor functions if the interest packet is arrived without execution. 
## System structure
An assumed system structure is shown in the figure. 
- **Delegator**: Schedule each function using Workflow information(JSON file) and node informatino(JSON file). 
- **File server**: It provides input files as necessary through FTP and SSH protocols. 
- **Docker repository(private)**: It has dockar images, and it is pulled from nodes as necessary. (if no docker container is loaded on the node). 
- **Set of nodes**:  The sef of VMs and physical nodes to which IP addresses are allocated. 

![sflow](https://user-images.githubusercontent.com/4952618/91002061-898bf880-e608-11ea-88a0-2b4f46aa163a.png)
## Setup
### Config. file（Deployed on delegator and nodes）
- Config. file is **nclw2.properties**, and is configured as follows: 
~~~
# the number of threads for sending/receiving data.
# never used...
rmgr_send_thread_num=10
rmgr_recv_thread_num=10

# sec(times) of the total count of cpu usages.
# never used...
total_cnt_cpuusage=10

# Target port number of data send/receive. 
port_number=8088
#8088
# IP address of delegator
#delegator_ip=192.168.1.15
delegator_ip=

# 0: SF-CUV 1: HEFT 2: FWS 3: CoordVNF 4: HClustering 5: PEFT
sched_algorithm=0

# "ftp" or "scp" can be chosen.
input_file_transfer_protocol=scp
# type the hostname or IP address of the ftp/scp server.
#ftp_server_ip=192.168.1.15
#ftp_server_id=user
ftp_server_ip=
ftp_server_id=
ftp_server_pass=

# /home/user/...
#ftp_server_homedirName=/home/user/files
ftp_server_homedirName=

# The private docker repository IP
#docker_repository_ip=192.168.1.15
docker_repository_ip=

# Home directory of the docker repository.
#docker_repository_home=/home/kanemih/nclw_images
docker_repository_home=
# ssh login ID/pass for the docker repository.
docker_repository_userid=
docker_repository_password=

# tmp directory to save the downloaded docker image for each node. 
# if the value is "docker_tar" the absolute path is $NCLW_HOME/docker_tar and 
# All docker image is stored in the directory. 
#docker_localdir=docker_tar
docker_localdir=

# In the ICN-SFC mode, interest/data is done by asynchronous channel(0) or server socket (1). 
# Currently, the mode of "1" works. 
#0: asynchronous 1: socket
ccn_comm_mode=1
## Strategy of NDN
#0: BestRouteStrategy 1: BackTrackStrategy 2: AutoICNSFCStrategy
nfd.strategy=2

#### Autonomous ICN ######

# Number of Nighborhood nodes for each node.
ccn_node_routernum=10

# max face num for each node.
ccn_node_face_num_max=100

# FIB max
ccn_fib_entry_max=80



# Routing
# 0: Base 1: AutoICN-SFCRouting
ccn_routing=1

#BradcastAddress for AutoICN-SFC.
ccn_bcastaddress=192.168.1.255
ccn_networkaddress=192.168.1.0
ccn_bcastport=15454
#ms
ccn_connection_timeout=2000
# max faces for each FIB entry.
ccn_fib_maxfaces_entry=5
~~~ 
### Env. config file@Delegator
- Environment file(JSON) is shown as follows. Sample file is **nclw/env_ncl.json**.
- In the following example, we assume that there is one data center (cloud) and one physical node in the cloud. The node has two CPU cores, and hyper-threading is enable for each core. Thus two vCPUs (virtual CPUs) can run simultaneously on a core. 
- The physical node has two VMs, and the first VM is mapped to two vCPUs and the second one is mapped to one vCPU.
- In this case, **vCPU ID: 0^0^0^1^0** is idle state, and others are used for processing on the VM.
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
### Config. file of a workflow@delegator.
- Workflow job cofig. file(JSON) is as follows. Example files are **nclw/job_ncl.json** or **nclw/job_ffmpeg.json**.
- The following example is nclw/ffmpeg.json. 
- **$R^FILE**: An required file to process (input file), and the node tries to download from the file server if it does not have it. 
- **$F^Predecessor SF** The function requires the data as the incomming data from its predecessor SFs.
- Sccessor SFs are specified by **dest_task_id**. **$F_Predecesssor SF** is defined at the destination SFs specified by "dest_task_id".
- **DUMMY** part is ignored if cmd is defined in docker entorypoint. Actually, the second and following parts are passed to the docker entirypoint.
- **root** is job ID, and each SF is defined as its child parts. 
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
- For example, if the SF has task_id = 2, the DockerFile is as follows. 
~~~
FROM alpine:latest
WORKDIR /
RUN apk --update add ffmpeg && \
    mkdir mnt_point && apk --update add zip
COPY docker-entrypoint.sh ./
ENTRYPOINT ["./docker-entrypoint.sh"]
~~~
And docker-entrypoint.sh is described as follows. 
~~~
#!/bin/sh

ffmpeg $1 $2 $3 $4 $5
zip $6 $7
~~~
Then the parameters defined in the subsequent parts of "DUMMY" is passed to docker-entrypoint.sh. 
### How to create a docker image and deploy to the docker repository in SFLow. 
- Create the image file (.tar) by `docker save IMAGE NAME -o NAME.tar` in the directory having DockerFile and docker-entropoint.sh. 
- Then upload the tar file to the location specified by `docker_repository_home` in the docker repository. Note that both IMAGE NAME and NAME are the value of **docker-imagename**. 

## How to use
### 0. Generating JSON file as the scheduling result (mapping result). 
- The cmd is `./schedgen.sh [env. file] [job file] [output path of the JSON file]`. 
- For example, by executing `./schedgen.sh nclw/env.json job_ffmpeg.json ret.json`, SFlow loads nclw/env.json and nclw_job_ffmpeg.json and performs the scheduling, thereby **ret.json** is generated as the output mapping file. 
- The format of the output JSON file is, **Each function (SF) in task_list, and each SF has the allocated node IP(ip)，vCPUID(vcpu_id), and VM ID(vm_id)**. 
### 1. IP-based SFC
#### 1.1 Start the process
1. Run `./nclw_startworkder.sh` at the delegator. Then all nodes in **nclw_hosts** run its required proecesses．As a result every node comes to wait for data. 
2. Run `./nclw_delegator.sh` at the delegator, then a scheduling algorithm is performed. Both the mapping information and environment information are sent to the START SF(i.e., the node to which the START SF is allocated). Then all SFs are processed according to the scheduling result. The result from the END SF is returned to the delegator. 
3. All stop: run `./nclw_stopworker.sh` at the delegator. Then all processes of SFlow are stopped at all nodes. 

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

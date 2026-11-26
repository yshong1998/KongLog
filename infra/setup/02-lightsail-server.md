# 2. Lightsail 서버

블로그를 올릴 서버를 만들고, 그 안에 스왑과 Docker를 준비한 기록입니다. 콘솔에서 하는 일(서버 만들기, 고정 IP, 방화벽)과 SSH 접속은 사람이 직접 했고, 서버 안의 명령도 사람이 직접 돌렸습니다.

서버 주소, 키, 비밀번호는 적지 않습니다. 명령에 서버 주소가 필요한 곳은 `<고정 IP>`로 적습니다.

## 정한 값

| 항목 | 값 |
| --- | --- |
| 만든 날 | 2026-11-26 |
| 지역 | 시드니 |
| 플랜 | 월 12달러. 메모리 2GB, vCPU 2개, SSD 60GB |
| 운영체제 | Ubuntu 24.04 LTS (만든 날 기준 24.04.5) |
| 네트워크 | 듀얼 스택(IPv4와 IPv6를 모두 씁니다) |
| 고정 IP | 붙였습니다 |
| SSH 키 | Lightsail 콘솔에서 만든 키 |
| 접속 계정 | `ubuntu` |
| 밖에 연 포트 | TCP 22(SSH), 80(HTTP), 443(HTTPS)과 UDP 443 |
| 스왑 | 2GB, `vm.swappiness=10` |
| Docker | 29.10.1 |
| Docker Compose | v5.8.0 |

## 1. 서버 만들기

Lightsail 콘솔에서 인스턴스를 만듭니다.

| 고르는 곳 | 고른 값 |
| --- | --- |
| 지역 | 시드니 |
| 플랫폼 | Linux/Unix |
| 블루프린트 | "OS 전용"의 Ubuntu 24.04 LTS |
| SSH 키 | 콘솔에서 새로 만들고 비밀 키 파일(`.pem`)을 내려받습니다 |
| 네트워크 | 듀얼 스택 |
| 플랜 | 월 12달러 (메모리 2GB, vCPU 2개, SSD 60GB) |
| 자동 스냅샷 | 켜지 않습니다 |

- 지역은 시드니입니다. 무료 플랜 계정에서는 시드니만 고를 수 있었습니다([1. AWS 계정과 도메인](01-aws-account-and-domain.md)). 독자가 한국에 있어 서울이 더 가깝지만 고를 수 없었습니다.
- 플랜에 "첫 90일 무료" 같은 표시는 없었습니다. 첫 달부터 요금이 크레딧에서 빠진다고 보고 계산합니다.
- 비밀 키 파일은 만들 때 한 번만 내려받을 수 있습니다. 잃어버리면 다시 받을 수 없으므로 안전한 곳에 둡니다. 저장소, 메신저, 메일에 올리지 않습니다.

- 운영체제를 Ubuntu 24.04 LTS로 고른 이유: Docker가 공식 지원하는 버전이고 2029년까지 보안 업데이트가 나옵니다. 만든 날 기준으로 Lightsail 문서의 목록에는 Ubuntu 24와 22만 있고 26.04는 없었습니다.
- 자동 스냅샷(서버 디스크를 통째로 복사해 두는 기능)은 켜지 않았습니다. 따로 요금이 붙습니다. 복구는 DB 백업으로 합니다.

## 2. 고정 IP 붙이기

고정 IP는 서버를 껐다 켜도 바뀌지 않는 주소입니다. 도메인이 이 주소를 가리키게 되므로 바뀌면 안 됩니다.

1. Lightsail 콘솔의 "네트워킹"에서 고정 IP를 만듭니다. 지역은 서버와 같아야 합니다.
2. 만든 고정 IP를 서버 인스턴스에 붙입니다.

- 인스턴스에 붙어 있는 동안은 요금이 없습니다. 떼어 둔 채로 두면 요금이 나옵니다. 서버를 지울 때 고정 IP도 함께 지웁니다.

## 3. 방화벽

Lightsail의 방화벽은 서버 밖에 있습니다. 여기서 열지 않은 포트는 서버까지 닿지 않습니다. 인스턴스 화면의 "네트워킹" 탭에서 정합니다.

기본으로 SSH(22)와 HTTP(80)가 열려 있습니다. HTTPS와 UDP 443을 더해 아래처럼 만듭니다.

| 종류 | 프로토콜 | 포트 | 허용 주소 |
| --- | --- | --- | --- |
| SSH | TCP | 22 | Anywhere IPv4, Anywhere IPv6 |
| HTTP | TCP | 80 | Anywhere IPv4, Anywhere IPv6 |
| HTTPS | TCP | 443 | Anywhere IPv4, Anywhere IPv6 |
| Custom | UDP | 443 | Anywhere IPv4, Anywhere IPv6 |

규칙을 넣을 때 허용 주소(Source IP addresses)를 Custom, Anywhere IPv4, Anywhere IPv6 중에서 고릅니다.

- Anywhere IPv4는 `0.0.0.0/0`, 곧 모든 IPv4 주소입니다. 블로그는 누구나 열어야 하므로 주소를 제한하지 않습니다.
- IPv4로 넣은 규칙은 IPv6에 적용되지 않습니다. 같은 규칙을 Anywhere IPv6(`::/0`)로 한 번 더 넣습니다.
- Custom은 특정 주소에서만 들어오게 할 때 씁니다. 여기서는 쓰지 않습니다.

- 443의 UDP는 HTTP/3(더 빠른 접속 방식)가 씁니다. `server/compose.yaml`의 Caddy가 이 포트를 엽니다.
- SSH는 접속하는 주소를 제한하지 않았습니다. 집의 IP는 바뀌고, 자동 배포도 매번 다른 주소에서 접속하기 때문입니다. 대신 키로만 접속할 수 있습니다. 비밀번호 접속은 처음부터 꺼져 있었습니다.
- 서버 안의 방화벽(`ufw`)은 켜지 않았습니다. 처음부터 꺼져 있었고 그대로 두었습니다. Docker가 연 포트는 `ufw` 규칙을 거치지 않아서, 켜도 컨테이너의 포트를 막지 못합니다. 막는 곳은 Lightsail 방화벽 하나로 둡니다.

서버에서 두 가지를 확인한 명령과 출력입니다.

```bash
sudo ufw status
sudo sshd -T | grep -i passwordauthentication
```

```
Status: inactive
passwordauthentication no
```

## 4. 접속

서버를 만들 때 내려받은 비밀 키 파일로 접속합니다. 계정은 `ubuntu`입니다.

```bash
ssh -i <비밀 키 파일> ubuntu@<고정 IP>
```

브라우저로도 접속할 수 있습니다. 인스턴스 화면에서 "SSH를 사용하여 연결"을 누르면 브라우저 안에 터미널이 열립니다. Lightsail이 따로 관리하는 키를 쓰므로 키 파일이 없는 기기에서도 됩니다.

접속한 뒤 상태를 봅니다.

```bash
lsb_release -ds; nproc; free -h; df -h /; swapon --show
```

만든 직후의 출력입니다.

```
Ubuntu 24.04.5 LTS
2
               total        used        free      shared  buff/cache   available
Mem:           1.9Gi       394Mi       733Mi       2.7Mi       949Mi       1.5Gi
Swap:             0B          0B          0B
Filesystem      Size  Used Avail Use% Mounted on
/dev/root        58G  2.5G   55G   5% /
```

`swapon --show`에는 아무것도 나오지 않았습니다. 스왑이 없다는 뜻입니다.

## 5. 스왑 켜기

스왑은 메모리가 모자랄 때 디스크의 일부를 메모리처럼 쓰는 것입니다. 2GB 서버에 네 가지를 올리므로, 메모리가 잠깐 넘칠 때 프로그램이 죽는 대신 느려지게 합니다.

한 번만 돌립니다. 두 번 돌리면 `/etc/fstab`에 같은 줄이 겹칩니다.

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
echo 'vm.swappiness=10' | sudo tee /etc/sysctl.d/99-konglog-swap.conf
sudo sysctl -p /etc/sysctl.d/99-konglog-swap.conf
```

| 명령 | 하는 일 |
| --- | --- |
| `fallocate` | 2GB짜리 빈 파일을 만듭니다 |
| `chmod 600` | 관리자만 읽게 합니다. 메모리에 있던 내용이 이 파일에 적히기 때문입니다 |
| `mkswap`, `swapon` | 파일을 스왑으로 만들고 켭니다 |
| `/etc/fstab`에 한 줄 추가 | 서버를 다시 켜도 스왑이 켜지게 합니다 |
| `vm.swappiness=10` | 스왑을 얼마나 일찍 쓰기 시작할지 정합니다 |

정한 이유입니다.

- 크기 2GB: 컨테이너 메모리 상한의 합이 1,408MB이고([README.md](../README.md)의 "메모리 상한"), 아무것도 띄우지 않은 서버가 432MB를 씁니다. 더하면 1,840MB로 서버 메모리 1.9GB에 거의 닿습니다. 디스크는 55GB가 비어 있어 2GB를 떼어도 부담이 없습니다.
- `swappiness` 10: 기본값은 60입니다. 스왑은 디스크라 메모리보다 훨씬 느려서, 메모리가 거의 찼을 때만 쓰도록 낮췄습니다.

## 6. Docker 설치

Docker 공식 문서의 순서대로 Docker의 저장소를 등록하고 설치합니다.

```bash
sudo apt update
sudo apt install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

sudo tee /etc/apt/sources.list.d/docker.sources <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}")
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF

sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker ubuntu
```

- `<<EOF`부터 `EOF`까지는 한 덩어리로 붙여 넣습니다.
- Docker Compose는 `docker-compose-plugin`으로 함께 설치됩니다. 명령은 `docker compose`입니다.
- 마지막 줄은 `ubuntu` 계정이 `sudo` 없이 `docker`를 쓰게 합니다. 다시 접속해야 반영됩니다. 이 계정은 이미 `sudo`를 암호 없이 쓰므로 권한이 더 넓어지지는 않습니다.
- 서버에서는 이미지를 받아 띄우기만 합니다. 이미지를 만드는 일은 서버에서 하지 않습니다. 메모리가 모자랍니다.

## 7. 다시 켜고 확인

스왑과 Docker가 서버를 다시 켠 뒤에도 켜져 있는지 봅니다.

```bash
sudo reboot
```

1분쯤 뒤에 다시 접속해서 확인합니다.

```bash
swapon --show; free -h; cat /proc/sys/vm/swappiness
docker --version; docker compose version; systemctl is-enabled docker
docker run --rm hello-world
```

다시 켠 뒤의 출력입니다.

```
NAME      TYPE SIZE USED PRIO
/swapfile file   2G   0B   -1
               total        used        free      shared  buff/cache   available
Mem:           1.9Gi       432Mi       1.2Gi       2.8Mi       435Mi       1.4Gi
Swap:          2.0Gi          0B       2.0Gi
10
Docker version 29.10.1, build c48360f
Docker Compose version v5.8.0
enabled
```

`docker run --rm hello-world`는 `Hello from Docker!`로 시작하는 안내문을 냈습니다. `sudo` 없이 돌았습니다.

## 8. 방화벽이 실제로 막는지 확인

방화벽에서 연 포트(80)와 열지 않은 포트(8080)에 시험용 웹 서버를 함께 띄우고, 밖에서 둘 다 불러 봅니다. 서버 안에서는 두 포트가 똑같이 열려 있으므로, 결과가 다르면 Lightsail 방화벽이 막은 것입니다.

서버에서 띄웁니다.

```bash
docker run --rm -d --name fwtest -p 80:80 -p 8080:80 caddy:2.11
```

내 컴퓨터에서 불러 봅니다. `-m 5`는 5초 안에 답이 없으면 그만두라는 뜻입니다.

```bash
curl -m 5 -I http://<고정 IP>/
curl -m 5 -I http://<고정 IP>:8080/
```

Windows의 PowerShell에서는 `curl` 대신 `curl.exe`로 씁니다. 결과입니다.

```
HTTP/1.1 200 OK
Server: Caddy
```

```
curl: (28) Connection timed out after 5003 milliseconds
```

80은 답했고 8080은 닿지 않았습니다. Docker가 서버에서 포트를 열어도 Lightsail 방화벽에서 열지 않은 포트는 밖에서 들어오지 못합니다.

확인한 뒤 서버에서 내립니다.

```bash
docker stop fwtest
```

- 443은 이 방법으로 확인하지 않았습니다. HTTPS 인증서가 있어야 해서, 도메인을 연결할 때 확인합니다.
- 8080 하나만 불러 봤습니다. 다른 포트는 방화벽 규칙 목록으로 확인했습니다.

## 잰 값

| 항목 | 값 |
| --- | --- |
| 서버 메모리 | 1.9GiB |
| 만든 직후 쓰는 메모리 | 394MiB |
| Docker를 설치하고 다시 켠 뒤 쓰는 메모리 | 432MiB |
| 디스크 | 58GB 중 2.5GB 사용 |

컨테이너별 메모리는 서버에 네 가지를 처음 띄울 때 다시 잽니다.

## 참고한 문서

2026-11-26에 확인했습니다.

- [Ubuntu에 Docker Engine 설치](https://docs.docker.com/engine/install/ubuntu/)
- [Docker 설치 뒤에 하는 설정](https://docs.docker.com/engine/install/linux-postinstall/)
- [Lightsail 방화벽](https://docs.aws.amazon.com/lightsail/latest/userguide/understanding-firewall-and-port-mappings-in-amazon-lightsail.html)
- [Lightsail 인스턴스 이미지 고르기](https://docs.aws.amazon.com/lightsail/latest/userguide/compare-options-choose-lightsail-instance-image.html)
- [Lightsail 고정 IP](https://docs.aws.amazon.com/lightsail/latest/userguide/understanding-static-ip-addresses-in-amazon-lightsail.html)
- [Lightsail의 SSH 키](https://docs.aws.amazon.com/lightsail/latest/userguide/understanding-ssh-in-amazon-lightsail.html)

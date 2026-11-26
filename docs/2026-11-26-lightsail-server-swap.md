---
title: "2GB 서버에 스왑 2GB를 켜고, 방화벽은 Lightsail 한 곳에만 두었다"
date: 2026-11-26
stage: 2
type: 결정
tags: [Lightsail, 스왑, Docker, 방화벽, ufw]
status: 메모
related: 이슈 #59, 풀 리퀘스트 #68, 커밋 95a4f07, infra/setup/02-lightsail-server.md, infra/README.md
---

## 한 줄 요약

Lightsail 2GB 서버(시드니, Ubuntu 24.04)에 스왑 2GB를 `swappiness` 10으로 켜고 Docker를 설치했다. 포트를 막는 곳은 서버 밖의 Lightsail 방화벽 하나로 두고, 시험용 서버를 80과 8080에 함께 띄워 실제로 막히는지 확인했다.

## 상황

2단계(배포)에서 블로그를 올릴 서버를 처음 만들었다. 이 서버 한 대에 Caddy, Next.js, Spring Boot, PostgreSQL을 함께 띄운다. 기획은 "메모리 상한을 정하고 스왑을 켠다"까지 정했고, 스왑 크기는 이슈 #59에서 #57의 메모리 측정값을 보고 정하기로 했다.

콘솔에서 하는 일(서버 만들기, 고정 IP, 방화벽)과 서버 안의 명령은 모두 사람이 직접 했다. Claude 세션은 순서와 명령을 건네고 결과를 받아 적었다.

서버를 만든 직후의 상태.

```
Ubuntu 24.04.5 LTS
2
               total        used        free      shared  buff/cache   available
Mem:           1.9Gi       394Mi       733Mi       2.7Mi       949Mi       1.5Gi
Swap:             0B          0B          0B
Filesystem      Size  Used Avail Use% Mounted on
/dev/root        58G  2.5G   55G   5% /
```

## 선택지

### 스왑 크기

- 숫자: 컨테이너 메모리 상한(`mem_limit`)의 합이 1,408MB(backend 640, frontend 384, db 256, caddy 128). 서버 메모리는 1.9GiB.
- #57에서는 운영체제가 약 380MB를 쓴다고 가정했다. 서버에서 재 보니 만든 직후 394MiB, Docker를 설치하고 다시 켠 뒤 432MiB였다. 가정보다 50MB쯤 많다.
- 1,408 + 432 = 1,840MB. 네 컨테이너가 동시에 상한까지 차면 1.9GiB에 거의 닿는다.

1. **스왑 없음**: 메모리가 넘치면 운영체제가 프로그램 하나를 골라 죽인다. 여유가 100MB 안팎이라 위험하다. 기획과도 다르다.
2. **1GB**: 잠깐 넘치는 것은 받는다. 디스크를 아낄 이유가 없다(55GB가 비어 있다).
3. **2GB** (메모리와 같은 크기): 컨테이너 상한의 합(1,408MB)보다 크다.
4. **4GB**: 이만큼 스왑을 쓰는 상황이면 이미 서버가 너무 느려서 쓸 수 없다. 그때는 플랜을 올려야 한다.

### swappiness

스왑을 얼마나 일찍 쓰기 시작할지 정하는 값. Ubuntu 기본값은 60.

- 60: 메모리가 남아 있어도 덜 쓰는 것을 스왑으로 내보낸다.
- 10: 메모리가 거의 찼을 때만 쓴다.

### 서버 안의 방화벽(ufw)

1. **Lightsail 방화벽만 쓴다**: 서버 밖에서 막는다. 콘솔에서 본다.
2. **ufw도 켠다**: 두 겹으로 막는다. 그런데 Docker 공식 문서에 Docker로 연 컨테이너 포트는 ufw 규칙을 거치지 않는다고 적혀 있다. 켜도 가장 막고 싶은 것(컨테이너 포트)은 못 막는다.

## 고른 것과 이유

### 스왑 2GB, swappiness 10

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
echo 'vm.swappiness=10' | sudo tee /etc/sysctl.d/99-konglog-swap.conf
sudo sysctl -p /etc/sysctl.d/99-konglog-swap.conf
```

출력.

```
Setting up swapspace version 1, size = 2 GiB (2147479552 bytes)
/swapfile none swap sw 0 0
vm.swappiness=10
vm.swappiness = 10
```

- 스왑은 디스크라 메모리보다 훨씬 느리다. 평소에는 쓰지 않고, 넘칠 때 죽는 대신 느려지게 하는 안전망으로만 쓰려고 10으로 낮췄다.
- `/etc/fstab`의 한 줄이 서버를 다시 켜도 스왑이 켜지게 한다. 이 줄이 없으면 `swapon`은 다시 켤 때 사라진다.

`sudo reboot` 뒤에 확인한 출력.

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

### 방화벽은 Lightsail 하나

Lightsail 방화벽에 연 것: TCP 22(SSH), 80(HTTP), 443(HTTPS), UDP 443(HTTP/3). 각각 Anywhere IPv4와 Anywhere IPv6로 넣었다. ufw는 처음부터 꺼져 있었고 그대로 뒀다.

```
$ sudo ufw status
Status: inactive
$ sudo sshd -T | grep -i passwordauthentication
passwordauthentication no
```

SSH는 접속하는 주소를 제한하지 않았다. 집의 IP는 바뀌고, 자동 배포(#61)도 매번 다른 주소에서 접속한다. 비밀번호 접속은 처음부터 꺼져 있어서 키로만 된다.

### 방화벽이 실제로 막는지 확인한 방법

서버에 아무것도 떠 있지 않으면 밖에서 불러 봐도 "방화벽이 막은 것"과 "받는 프로그램이 없는 것"을 구분하기 어렵다. 그래서 시험용 웹 서버 하나를 연 포트(80)와 열지 않은 포트(8080)에 함께 띄웠다. 서버 안에서는 두 포트가 똑같이 열려 있으므로, 결과가 다르면 Lightsail 방화벽이 막은 것이다.

```bash
# 서버에서
docker run --rm -d --name fwtest -p 80:80 -p 8080:80 caddy:2.11
```

```
PS> curl.exe -m 5 -I http://<고정 IP>/
HTTP/1.1 200 OK
Accept-Ranges: bytes
Content-Length: 18753
Content-Type: text/html; charset=utf-8
Server: Caddy

PS> curl.exe -m 5 -I http://<고정 IP>:8080/
curl: (28) Connection timed out after 5003 milliseconds
```

Docker가 서버에서 `0.0.0.0:8080`으로 포트를 열어도 Lightsail 방화벽에서 열지 않았으면 밖에서 닿지 않는다. `server/compose.yaml`에 실수로 DB 포트를 열어도 한 번은 막아 준다는 뜻이다.

## 예상과 달랐던 것

- **운영체제가 쓰는 메모리**: 가정 380MB, 실제 432MiB(Docker 설치 뒤).
- **방화벽 화면**: AWS 문서에는 "IPv4 방화벽과 IPv6 방화벽이 독립적이고 따로 설정한다"고 적혀 있어서 탭이 둘인 줄 알았다. 실제 화면은 규칙 하나를 넣을 때 허용 주소(Source IP addresses)를 Custom, Anywhere IPv4, Anywhere IPv6 중에서 고르는 방식이었다. 기본값은 `0.0.0.0/0`(모든 IPv4 주소).
- **"첫 90일 무료" 표시**: 기획 문서에는 이 플랜이 첫 3개월 무료 대상이라고 적혀 있었지만 화면에 그런 표시는 없었다. 첫 달부터 월 12달러가 크레딧에서 빠진다고 보고 계산한다.
- **SSH 키**: 처음 계획은 내 컴퓨터에서 키를 만들어 공개 키만 올리는 것이었다(ed25519 키를 올릴 수 있는지는 문서에서 확인하지 못했다). 실제로는 콘솔에서 키를 만들어 `.pem`을 내려받았다. 작업하던 기기에 `.pem`이 없어서 서버 안의 명령은 Lightsail의 브라우저 SSH로 돌렸다. 브라우저 SSH는 Lightsail이 따로 관리하는 키를 써서 키 파일이 없어도 된다.
- **Lightsail의 Ubuntu 목록**: 문서 기준으로 Ubuntu 24와 22만 있고 26.04는 없었다.

## 감수한 것

- 스왑을 실제로 쓰게 되면 느려진다. 스왑은 죽지 않게 하는 것이지 메모리를 늘리는 것이 아니다. 스왑 사용량이 계속 0이 아니면 상한을 다시 맞추거나 4GB 플랜(월 24달러)을 검토한다.
- SSH가 모든 주소에 열려 있다. 키로만 접속되지만 접속 시도 자체는 계속 들어온다.
- 서버 안에는 방화벽이 없다. Lightsail 방화벽 규칙을 잘못 열면 바로 밖에 드러난다.

아직 확인하지 않은 것.

- 메모리가 실제로 모자랄 때 스왑이 어떻게 쓰이는지. 컨테이너 네 개를 서버에 처음 띄우는 #60에서 다시 잰다.
- 스왑을 켠 뒤 `mem_limit`이 어떻게 동작하는지. Docker 문서에는 메모리 상한만 정하고 스왑 상한을 정하지 않으면 컨테이너가 상한과 같은 양의 스왑을 더 쓸 수 있다고 적혀 있다. 그렇다면 상한을 넘은 컨테이너가 바로 다시 뜨지 않고 스왑을 쓰며 느려질 수 있다. 서버에서 확인하지 않았다.
- 443이 밖에서 열리는지. HTTPS 인증서가 있어야 해서 #60에서 확인한다.
- 닫힌 포트는 8080 하나만 불러 봤다.

## 배운 점

- 측정값은 그 환경에서 다시 재야 한다. 내 컴퓨터에서 정한 가정(380MB)이 서버에서는 432MiB였다.
- "막혀 있다"를 확인하려면 열린 것과 막힌 것을 같은 조건에서 나란히 불러 봐야 한다. 닫힌 포트만 불러서는 왜 안 되는지 알 수 없다.
- ufw를 켜면 안전해질 것 같지만 Docker의 포트는 ufw를 거치지 않는다. 막는 곳이 어디인지 알고 한 곳에 두는 편이 낫다.
- 문서의 설명과 콘솔 화면은 다를 수 있다. 화면에서 한 일은 화면에서 본 대로 적는다.

## 글로 쓸 때 강조할 것

- 2GB 서버에서 숫자를 더해 보는 과정: 상한의 합 1,408MB + 서버 432MB = 1,840MB.
- 80과 8080을 함께 띄워 방화벽을 확인한 방법. 출력 두 개가 나란히 있으면 한눈에 보인다.
- "ufw를 왜 안 켰나"는 질문에 대한 답.

# 프론트엔드(Next.js) 이미지. 저장소 루트에서 만든다. 마지막 인자 frontend가 빌드 대상 폴더다.
#   docker build -f infra/docker/frontend.Dockerfile -t konglog-frontend:local frontend
# 백엔드 주소는 만들 때가 아니라 띄울 때 환경 변수 API_BASE_URL로 정한다. 방법은 infra/README.md에 있다.
# 이미지에 넣지 않을 파일은 옆의 frontend.Dockerfile.dockerignore에 적는다.

# 1단계: 빌드에 필요한 패키지를 모두 받는다(개발용 포함).
FROM node:22-bookworm-slim AS deps
WORKDIR /app
# 패키지 목록만 먼저 복사한다. 코드만 바뀐 빌드에서는 이 단계를 다시 하지 않는다.
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci

# 2단계: 배포용으로 빌드한다. 백엔드가 떠 있지 않아도 된다.
FROM node:22-bookworm-slim AS build
WORKDIR /app
ENV NEXT_TELEMETRY_DISABLED=1
COPY --from=deps /app/node_modules ./node_modules
COPY . .
# public 폴더가 없어도 마지막 단계의 복사가 실패하지 않게 빈 폴더를 만들어 둔다.
RUN mkdir -p public && npm run build

# 3단계: 실행에 필요한 패키지만 받는다. 타입 검사, 코드 검사 도구는 뺀다.
FROM node:22-bookworm-slim AS prod-deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --omit=dev

# 4단계: 실행에 필요한 것만 담는다. 소스 코드는 들어가지 않는다.
FROM node:22-bookworm-slim
WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
# 컨테이너 밖에서 들어오는 요청을 받으려면 localhost가 아니라 0.0.0.0에서 들어야 한다.
ENV HOSTNAME=0.0.0.0
ENV PORT=3000

COPY --from=prod-deps /app/node_modules ./node_modules
COPY --from=build /app/package.json /app/next.config.ts ./
COPY --from=build /app/public ./public
# Next.js가 실행 중에 캐시를 .next 아래에 쓰므로 실행 계정이 쓸 수 있게 한다.
# 빌드할 때 쓴 캐시(.next/cache)는 실행에 필요 없어서 .dockerignore가 아니라 여기서 뺀다.
COPY --from=build --chown=node:node --exclude=cache /app/.next ./.next

# root가 아닌 계정으로 실행한다. node 계정은 기반 이미지에 들어 있다.
USER node

EXPOSE 3000
# npm을 거치지 않고 바로 실행한다. 멈추라는 신호가 Next.js에 바로 전달된다.
CMD ["node", "node_modules/next/dist/bin/next", "start"]

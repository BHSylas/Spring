## 로컬 실행 전 필수 설정 (.env)

1) 프로젝트 루트에서 `.env.example`을 복사해 `.env`를 만듭니다.
2) `.env`에 DB 정보와 JWT_SECRET을 채웁니다. (JWT_SECRET 최소 32자, 권장 64자 이상)
3) IntelliJ 사용 시 EnvFile 플러그인에서 `.env` 파일을 Run Configuration에 등록합니다.
4) `.env`는 커밋 금지입니다. (.gitignore 포함)

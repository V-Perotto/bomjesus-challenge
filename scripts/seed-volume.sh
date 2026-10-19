#!/usr/bin/env bash
# Cria alunos e professores em volume pela API, para medir a SC-004 (spec 002).
# Uso: ./scripts/seed-volume.sh            (1.000 alunos + 1.000 professores, 2 endereços cada)
#      COUNT=100 API_URL=http://localhost:8081/api ./scripts/seed-volume.sh
set -euo pipefail

API_URL="${API_URL:-http://localhost:8080/api}"
COUNT="${COUNT:-1000}"
PARALLEL="${PARALLEL:-8}"

ADDRESSES='[{"street":"Rua XV de Novembro, 100","city":"Curitiba","state":"PR","zipCode":"80020-310","country":"Brasil"},{"street":"Av. Sete de Setembro, 2000","city":"Curitiba","state":"PR","zipCode":"80230-010","country":"Brasil"}]'

post() {
    local resource="$1" body="$2"
    curl -sf -o /dev/null -X POST "$API_URL/$resource" -H 'Content-Type: application/json' -d "$body" \
        || { echo "Falha ao criar em /$resource: $body" >&2; return 1; }
}
export -f post
export API_URL ADDRESSES

echo "Criando $COUNT alunos e $COUNT professores em $API_URL ..."
seq 1 "$COUNT" | xargs -P "$PARALLEL" -I{} bash -c \
    'post students "{\"name\":\"Aluno Volume {}\",\"phoneNumber\":\"+55 41 90000-0000\",\"emailAddress\":\"aluno.volume.{}@escola.com\",\"studentNumber\":\"VOL-{}\",\"addresses\":$ADDRESSES}"'
seq 1 "$COUNT" | xargs -P "$PARALLEL" -I{} bash -c \
    'post professors "{\"name\":\"Professor Volume {}\",\"phoneNumber\":\"+55 41 3000-0000\",\"emailAddress\":\"professor.volume.{}@escola.com\",\"salary\":5000,\"addresses\":$ADDRESSES}"'
echo "Concluído."

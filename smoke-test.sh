#!/bin/bash
# Smoke test completo do HENRY Telemetry Service
# Uso: ./smoke-test.sh
set -e

BASE="https://localhost:8443"
SECRET="henry-payload-hmac-secret-ford-2026"
VIN="1FTFW1ET5EKE36050"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

ok()   { echo -e "${GREEN}[OK]${NC} $1"; }
info() { echo -e "${YELLOW}[..] $1${NC}"; }
fail() { echo -e "${RED}[FAIL]${NC} $1"; exit 1; }

# ── 1. Aguarda a API subir ────────────────────────────────────────────────────
info "Aguardando API ficar disponível em $BASE ..."
for i in $(seq 1 30); do
  curl -fsk "$BASE/api-docs" > /dev/null 2>&1 && break
  [ "$i" -eq 30 ] && fail "API não respondeu após 60s"
  sleep 2
done
ok "API disponível"

# ── 2. Auth — gera token como MECHANIC ───────────────────────────────────────
info "Gerando token JWT (role MECHANIC)..."
TOKEN_RESP=$(curl -sk -X POST "$BASE/api/v1/auth/token" \
  -H "Content-Type: application/json" \
  -d '{"subject":"mechanic-demo","role":"MECHANIC"}')

TOKEN=$(echo "$TOKEN_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")
[ -z "$TOKEN" ] && fail "Token não retornado — resposta: $TOKEN_RESP"
ok "Token obtido: ${TOKEN:0:30}..."

# ── 3. GET /readings — lista vazia (veículo recém criado) ────────────────────
info "GET /readings para VIN $VIN ..."
READ_RESP=$(curl -sk -H "Authorization: Bearer $TOKEN" \
  "$BASE/api/v1/vehicles/$VIN/readings")
echo "$READ_RESP" | python3 -m json.tool
ok "GET /readings OK"

# ── 4. POST /readings — com assinatura HMAC-SHA256 ───────────────────────────
info "Calculando X-Payload-Signature (HMAC-SHA256)..."
BODY='{"vin":"'"$VIN"'","engineTempCelsius":92.5,"oilLifePercent":65,"rpm":2100,"fuelLevelPercent":78,"faultCodes":"P0300"}'
SIG=$(echo -n "$BODY" | openssl dgst -sha256 -hmac "$SECRET" -binary | base64)
ok "Signature: $SIG"

info "POST /readings ..."
CREATE_RESP=$(curl -sk -X POST "$BASE/api/v1/vehicles/$VIN/readings" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Payload-Signature: $SIG" \
  -d "$BODY")
echo "$CREATE_RESP" | python3 -m json.tool
echo "$CREATE_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); exit(0 if 'id' in d else 1)" \
  || fail "POST /readings não retornou id"
ok "POST /readings OK — leitura criada"

# ── 5. GET /health-score ──────────────────────────────────────────────────────
info "GET /health-score ..."
HS_RESP=$(curl -sk -H "Authorization: Bearer $TOKEN" \
  "$BASE/api/v1/vehicles/$VIN/health-score")
echo "$HS_RESP" | python3 -m json.tool
ok "GET /health-score OK"

# ── 6. Valida que VIN inválido retorna 400 ────────────────────────────────────
info "Testando rejeição de VIN inválido..."
STATUS=$(curl -sk -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer $TOKEN" \
  "$BASE/api/v1/vehicles/VIN_INVALIDO/readings")
[ "$STATUS" = "400" ] || fail "Esperado 400 para VIN inválido, recebido $STATUS"
ok "VIN inválido retornou 400 corretamente"

# ── 7. Valida que request sem assinatura retorna 400 ─────────────────────────
info "Testando rejeição de POST sem X-Payload-Signature..."
STATUS=$(curl -sk -o /dev/null -w "%{http_code}" -X POST \
  "$BASE/api/v1/vehicles/$VIN/readings" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "$BODY")
[ "$STATUS" = "400" ] || fail "Esperado 400 para request sem assinatura, recebido $STATUS"
ok "Request sem assinatura retornou 400 corretamente"

# ── 8. Swagger UI acessível ───────────────────────────────────────────────────
info "Verificando Swagger UI..."
SW_STATUS=$(curl -sk -o /dev/null -w "%{http_code}" "$BASE/swagger-ui.html")
[ "$SW_STATUS" = "200" ] || [ "$SW_STATUS" = "302" ] || fail "Swagger retornou $SW_STATUS"
ok "Swagger UI acessível"

echo ""
echo -e "${GREEN}══════════════════════════════════════════${NC}"
echo -e "${GREEN}  Todos os testes passaram! API funcionando.${NC}"
echo -e "${GREEN}══════════════════════════════════════════${NC}"
echo ""
echo "  Swagger UI: $BASE/swagger-ui.html"
echo "  VIN de demo: $VIN"

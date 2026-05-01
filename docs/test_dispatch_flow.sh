#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# Do Delivery — Full Dispatch Flow Test Script
#
# Runs the entire lifecycle end-to-end:
#   Register → Login → Rider Online → Create Order → Assign → Accept → Pickup → Deliver
#
# Requires: curl, jq  (brew install jq)
# Usage:    bash docs/test_dispatch_flow.sh
# ─────────────────────────────────────────────────────────────────────────────

set -e
BASE="http://localhost:8080"
C_PHONE="+8801911111111"
R_PHONE="+8801922222222"

BOLD="\033[1m"
GREEN="\033[0;32m"
BLUE="\033[0;34m"
RED="\033[0;31m"
RESET="\033[0m"

header()  { echo -e "\n${BOLD}${BLUE}── $1 ──${RESET}"; }
ok()      { echo -e "${GREEN}✓ $1${RESET}"; }
fail()    { echo -e "${RED}✗ $1${RESET}"; exit 1; }
show()    { echo "$1" | jq . 2>/dev/null || echo "$1"; }

# ── helpers ──────────────────────────────────────────────────────────────────
register() {
  local name="$1" phone="$2" role="$3"
  curl -s -X POST "$BASE/api/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"$name\",\"phone\":\"$phone\",\"role\":\"$role\"}"
}

login() {
  local phone="$1" otp="$2"
  curl -s -X POST "$BASE/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"phone\":\"$phone\",\"otp\":\"$otp\"}"
}

refresh_token() {
  local rt="$1"
  curl -s -X POST "$BASE/api/auth/refresh" \
    -H "Content-Type: application/json" \
    -d "{\"refreshToken\":\"$rt\"}"
}

# ─────────────────────────────────────────────────────────────────────────────
header "STEP 1 — Register customer + rider"

C_REG=$(register "Test Customer" "$C_PHONE" "CUSTOMER")
show "$C_REG"
C_OTP=$(echo "$C_REG" | jq -r '.data.otp // empty')
[ -n "$C_OTP" ] && ok "Customer registered. OTP=$C_OTP" || fail "Customer registration failed"

R_REG=$(register "Test Rider" "$R_PHONE" "RIDER")
show "$R_REG"
R_OTP=$(echo "$R_REG" | jq -r '.data.otp // empty')
[ -n "$R_OTP" ] && ok "Rider registered. OTP=$R_OTP" || fail "Rider registration failed"

# ─────────────────────────────────────────────────────────────────────────────
header "STEP 2 — Login both and extract tokens"

C_LOGIN=$(login "$C_PHONE" "$C_OTP")
show "$C_LOGIN"
C_TOKEN=$(echo "$C_LOGIN" | jq -r '.data.accessToken // empty')
C_REFRESH=$(echo "$C_LOGIN" | jq -r '.data.refreshToken // empty')
[ -n "$C_TOKEN" ] && ok "Customer token obtained" || fail "Customer login failed"

R_LOGIN=$(login "$R_PHONE" "$R_OTP")
show "$R_LOGIN"
R_TOKEN=$(echo "$R_LOGIN" | jq -r '.data.accessToken // empty')
R_REFRESH=$(echo "$R_LOGIN" | jq -r '.data.refreshToken // empty')
[ -n "$R_TOKEN" ] && ok "Rider token obtained" || fail "Rider login failed"

# ─────────────────────────────────────────────────────────────────────────────
header "STEP 3 — Rider goes online"

ONLINE=$(curl -s -X POST "$BASE/api/riders/online" \
  -H "Authorization: Bearer $R_TOKEN")
show "$ONLINE"
STATUS=$(echo "$ONLINE" | jq -r '.status')
[ "$STATUS" = "200" ] && ok "Rider is online" || fail "Go online failed: $ONLINE"

# ─────────────────────────────────────────────────────────────────────────────
header "STEP 4 — Rider sends location"

LOCATION=$(curl -s -X POST "$BASE/api/riders/location" \
  -H "Authorization: Bearer $R_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"lat":23.9010,"lng":90.4010}')
show "$LOCATION"
STATUS=$(echo "$LOCATION" | jq -r '.status')
[ "$STATUS" = "200" ] && ok "Rider location updated (near pickup)" || fail "Location update failed: $LOCATION"

# ─────────────────────────────────────────────────────────────────────────────
header "STEP 5 — Customer creates order"

ORDER=$(curl -s -X POST "$BASE/api/orders" \
  -H "Authorization: Bearer $C_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "pickupLat": 23.9000,
    "pickupLng": 90.4000,
    "dropLat":   23.8103,
    "dropLng":   90.4125,
    "type":      "PARCEL",
    "note":      "dispatch flow test"
  }')
show "$ORDER"
ORDER_ID=$(echo "$ORDER" | jq -r '.data.id // empty')
ORDER_STATUS=$(echo "$ORDER" | jq -r '.data.status // empty')
[ -n "$ORDER_ID" ] && ok "Order created: id=$ORDER_ID status=$ORDER_STATUS" || fail "Order creation failed: $ORDER"

# ─────────────────────────────────────────────────────────────────────────────
header "STEP 6 — Customer assigns nearest rider"

ASSIGN=$(curl -s -X POST "$BASE/api/orders/$ORDER_ID/assign" \
  -H "Authorization: Bearer $C_TOKEN")
show "$ASSIGN"
ASSIGN_STATUS=$(echo "$ASSIGN" | jq -r '.data.status // empty')
RIDER_NAME=$(echo "$ASSIGN"   | jq -r '.data.riderName // empty')
[ "$ASSIGN_STATUS" = "ASSIGNED" ] && ok "Order ASSIGNED to rider: $RIDER_NAME" || fail "Assign failed: $ASSIGN"

# ─────────────────────────────────────────────────────────────────────────────
header "STEP 7 — Rider accepts order"

ACCEPT=$(curl -s -X POST "$BASE/api/orders/$ORDER_ID/accept" \
  -H "Authorization: Bearer $R_TOKEN")
show "$ACCEPT"
ACCEPT_STATUS=$(echo "$ACCEPT" | jq -r '.data.status // empty')
[ "$ACCEPT_STATUS" = "ACCEPTED" ] && ok "Order ACCEPTED by rider" || fail "Accept failed: $ACCEPT"

# ─────────────────────────────────────────────────────────────────────────────
header "STEP 8 — Rider marks pickup"

PICKUP=$(curl -s -X POST "$BASE/api/orders/$ORDER_ID/pickup" \
  -H "Authorization: Bearer $R_TOKEN")
show "$PICKUP"
PICKUP_STATUS=$(echo "$PICKUP" | jq -r '.data.status // empty')
[ "$PICKUP_STATUS" = "PICKED_UP" ] && ok "Order PICKED_UP" || fail "Pickup failed: $PICKUP"

# ─────────────────────────────────────────────────────────────────────────────
header "STEP 9 — Rider marks delivered"

DELIVER=$(curl -s -X POST "$BASE/api/orders/$ORDER_ID/deliver" \
  -H "Authorization: Bearer $R_TOKEN")
show "$DELIVER"
DELIVER_STATUS=$(echo "$DELIVER"  | jq -r '.data.status // empty')
DELIVERED_AT=$(echo "$DELIVER"    | jq -r '.data.deliveredAt // empty')
[ "$DELIVER_STATUS" = "DELIVERED" ] && ok "Order DELIVERED at $DELIVERED_AT" || fail "Deliver failed: $DELIVER"

# ─────────────────────────────────────────────────────────────────────────────
header "STEP 10 — Verify final order state"

FINAL=$(curl -s -X GET "$BASE/api/orders/$ORDER_ID" \
  -H "Authorization: Bearer $C_TOKEN")
show "$FINAL"

echo -e "\n${BOLD}═══════════════ SUMMARY ═══════════════${RESET}"
echo -e "Order ID       : $(echo "$FINAL" | jq -r '.data.id')"
echo -e "Final Status   : $(echo "$FINAL" | jq -r '.data.status')"
echo -e "Rider          : $(echo "$FINAL" | jq -r '.data.riderName')"
echo -e "Assigned At    : $(echo "$FINAL" | jq -r '.data.assignedAt')"
echo -e "Picked Up At   : $(echo "$FINAL" | jq -r '.data.pickedUpAt')"
echo -e "Delivered At   : $(echo "$FINAL" | jq -r '.data.deliveredAt')"
echo -e "${GREEN}${BOLD}All steps passed!${RESET}"

# ─────────────────────────────────────────────────────────────────────────────
header "BONUS — Test refresh token"

REFRESHED=$(refresh_token "$C_REFRESH")
show "$REFRESHED"
NEW_TOKEN=$(echo "$REFRESHED" | jq -r '.data.accessToken // empty')
[ -n "$NEW_TOKEN" ] && ok "Refresh token works — new access token obtained" || fail "Refresh token failed: $REFRESHED"

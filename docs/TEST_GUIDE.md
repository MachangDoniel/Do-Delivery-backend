# 🚚 Do Delivery Backend — Test Guide

Step-by-step verification guide for developers and testers using `curl`.  
For the full API reference see [API_REFERENCE.md](./API_REFERENCE.md).  
For an automated end-to-end script see [test_dispatch_flow.sh](./test_dispatch_flow.sh).

---

## Base URL

```
http://localhost:8080
```

---

## Quick Start (copy-paste script)

```bash
bash docs/test_dispatch_flow.sh
```

Runs the full dispatch lifecycle automatically and prints a summary.

---

# 1. AUTH FLOW

## 1.1 Register a CUSTOMER

```bash
curl -s -X POST http://localhost:8080/api/auth/register \
-H "Content-Type: application/json" \
-d '{
  "name":  "Doniel",
  "phone": "+8801700000001",
  "role":  "CUSTOMER"
}'
```

Expected: `201` — `otp` value returned in `data.otp` (mock dev mode)

---

## 1.2 Register a RIDER

```bash
curl -s -X POST http://localhost:8080/api/auth/register \
-H "Content-Type: application/json" \
-d '{
  "name":  "Karim Rider",
  "phone": "+8801700000002",
  "role":  "RIDER"
}'
```

Expected: `201` — separate `otp` for rider

---

## 1.3 Login (get JWT)

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
-H "Content-Type: application/json" \
-d '{
  "phone": "+8801700000001",
  "otp":   "<OTP_FROM_REGISTER>"
}'
```

Expected `200` response:

```json
{
  "status": 200,
  "data": {
    "accessToken":  "eyJhbGci...",
    "refreshToken": "eyJhbGci...",
    "expiresIn":    900
  }
}
```

> Save `accessToken` as `CUSTOMER_TOKEN` and `RIDER_TOKEN` for the respective accounts.

---

## 1.4 Refresh access token

```bash
curl -s -X POST http://localhost:8080/api/auth/refresh \
-H "Content-Type: application/json" \
-d '{ "refreshToken": "<REFRESH_TOKEN>" }'
```

Expected: `200` — new `accessToken` and `refreshToken`.  
Old refresh token is invalidated immediately.

---

# 2. ORDER FLOW

## 2.1 Create Order (CUSTOMER)

```bash
curl -s -X POST http://localhost:8080/api/orders \
-H "Content-Type: application/json" \
-H "Authorization: Bearer <CUSTOMER_TOKEN>" \
-d '{
  "pickupLat": 23.9999,
  "pickupLng": 90.4203,
  "dropLat":   23.8103,
  "dropLng":   90.4125,
  "type":      "PARCEL",
  "note":      "Pickup from Kaliganj, drop at Dhaka"
}'
```

Expected: `201` — `status: CREATED`, price calculated automatically.

```json
{
  "status": 201,
  "data": {
    "id":           "6cccc9f5-2ffb-4b0c-a2d2-19a7e73b8fde",
    "customerId":   "a944ebea-...",
    "customerName": "Doniel",
    "riderId":      null,
    "riderName":    null,
    "pickupLat":    23.9999,
    "pickupLng":    90.4203,
    "dropLat":      23.8103,
    "dropLng":      90.4125,
    "type":         "PARCEL",
    "status":       "CREATED",
    "price":        18.32,
    "note":         "Pickup from Kaliganj, drop at Dhaka",
    "assignedAt":   null,
    "pickedUpAt":   null,
    "deliveredAt":  null,
    "createdAt":    "2026-05-02T12:00:00Z",
    "updatedAt":    "2026-05-02T12:00:00Z"
  }
}
```

> Save `data.id` as `ORDER_ID`.

---

## 2.2 Get My Orders (CUSTOMER)

```bash
curl -s http://localhost:8080/api/orders \
-H "Authorization: Bearer <CUSTOMER_TOKEN>"
```

---

## 2.3 Get Order by ID

```bash
curl -s http://localhost:8080/api/orders/<ORDER_ID> \
-H "Authorization: Bearer <CUSTOMER_TOKEN>"
```

---

## 2.4 Cancel Order

```bash
curl -s -X POST http://localhost:8080/api/orders/<ORDER_ID>/cancel \
-H "Authorization: Bearer <CUSTOMER_TOKEN>"
```

Only works when `status = CREATED`. Returns `status: CANCELLED`.

---

# 3. RIDER DISPATCH FLOW

> State machine: `CREATED → ASSIGNED → ACCEPTED → PICKED_UP → DELIVERED`

## 3.1 Rider goes online

```bash
curl -s -X POST http://localhost:8080/api/riders/online \
-H "Authorization: Bearer <RIDER_TOKEN>"
```

Expected: `200`

---

## 3.2 Rider sends GPS location

The rider must send a location **before** being assignable.

```bash
curl -s -X POST http://localhost:8080/api/riders/location \
-H "Content-Type: application/json" \
-H "Authorization: Bearer <RIDER_TOKEN>" \
-d '{
  "lat": 23.9001,
  "lng": 90.4010
}'
```

Expected: `200`

---

## 3.3 Assign nearest rider (CUSTOMER)

```bash
curl -s -X POST http://localhost:8080/api/orders/<ORDER_ID>/assign \
-H "Authorization: Bearer <CUSTOMER_TOKEN>"
```

Expected: `200` — `status: ASSIGNED`, `riderId` and `assignedAt` populated.

Fails with `400` if no online riders have a known location.

---

## 3.4 Rider accepts

```bash
curl -s -X POST http://localhost:8080/api/orders/<ORDER_ID>/accept \
-H "Authorization: Bearer <RIDER_TOKEN>"
```

Expected: `200` — `status: ACCEPTED`

---

## 3.4b Rider rejects (optional)

```bash
curl -s -X POST http://localhost:8080/api/orders/<ORDER_ID>/reject \
-H "Authorization: Bearer <RIDER_TOKEN>"
```

Order reverts to `CREATED`, rider is unassigned. Customer must `/assign` again.

---

## 3.5 Rider marks pickup

```bash
curl -s -X POST http://localhost:8080/api/orders/<ORDER_ID>/pickup \
-H "Authorization: Bearer <RIDER_TOKEN>"
```

Expected: `200` — `status: PICKED_UP`, `pickedUpAt` set.

---

## 3.6 Rider marks delivered

```bash
curl -s -X POST http://localhost:8080/api/orders/<ORDER_ID>/deliver \
-H "Authorization: Bearer <RIDER_TOKEN>"
```

Expected: `200` — `status: DELIVERED`, `deliveredAt` set.

---

## 3.7 Rider goes offline

```bash
curl -s -X POST http://localhost:8080/api/riders/offline \
-H "Authorization: Bearer <RIDER_TOKEN>"
```

---

# 4. SECURITY TESTS

## 4.1 No token → 401

```bash
curl -s http://localhost:8080/api/orders
```

Expected: `401 Unauthorized`

---

## 4.2 Wrong role → 403

Use a RIDER token on a CUSTOMER-only endpoint:

```bash
curl -s -X POST http://localhost:8080/api/orders \
-H "Content-Type: application/json" \
-H "Authorization: Bearer <RIDER_TOKEN>" \
-d '{"pickupLat":23.9,"pickupLng":90.4,"dropLat":23.8,"dropLng":90.4,"type":"PARCEL"}'
```

Expected: `403 Forbidden`

---

## 4.3 Invalid state transition → 400

Try delivering before pickup:

```bash
curl -s -X POST http://localhost:8080/api/orders/<ORDER_ID>/deliver \
-H "Authorization: Bearer <RIDER_TOKEN>"
```

Expected: `400` — `"Cannot perform delivery. Order status is ACCEPTED. Expected: PICKED_UP"`

---

## 4.4 Wrong rider tries to accept → 400

Use a different rider token on an order assigned to another rider.

Expected: `400` — `"This order is not assigned to you"`

---

# 5. TROUBLESHOOTING

| Symptom | Cause | Fix |
|---------|-------|-----|
| `401 Unauthorized` | Missing or expired token | Re-login or call `/api/auth/refresh` |
| `403 Forbidden` | Wrong role | Use correct token (CUSTOMER vs RIDER) |
| `404 on /api/riders/...` | Old server build running | Kill port 8080 process and restart: `lsof -ti:8080 \| xargs kill -9` then `mvn -DskipTests spring-boot:run` |
| `400 No online riders` | Rider not online or no location sent | Call `/api/riders/online` then `/api/riders/location` first |
| `400 Order cannot be assigned` | Order not in `CREATED` state | Check current status with `GET /api/orders/<id>` |
| `500 Internal Server Error` | Unexpected fault | Check Spring Boot logs |

# Do Delivery — API Reference

**Base URL:** `http://localhost:8080`  
**Auth:** All protected routes require `Authorization: Bearer <accessToken>`  
**Content-Type:** `application/json`

---

## Response Envelope

Every response (success or error) uses this wrapper:

```json
{
  "status":    200,
  "data":      { ... },
  "error":     null,
  "message":   null,
  "timestamp": "2026-05-02T12:00:00Z",
  "path":      null
}
```

On error `data` is null; `error` and `message` are populated.

---

## Enums

### `Role`
| Value | Description |
|-------|-------------|
| `CUSTOMER` | Places and tracks orders |
| `RIDER` | Delivers orders |

---

### `OrderType`
| Value | Description |
|-------|-------------|
| `DOCUMENT` | Letters, contracts, IDs |
| `SMALL` | Small parcels, envelopes |
| `PARCEL` | Larger packages |

---

### `OrderStatus`
| Value | Description | Who triggers |
|-------|-------------|--------------|
| `CREATED` | Order placed, waiting for dispatch | System (on order creation) |
| `ASSIGNED` | Nearest rider selected by system | Customer via `/assign` |
| `ACCEPTED` | Assigned rider confirmed | Rider via `/accept` |
| `PICKED_UP` | Rider collected the item | Rider via `/pickup` |
| `DELIVERED` | Item delivered to recipient | Rider via `/deliver` |
| `CANCELLED` | Order cancelled | Customer via `/cancel` (only from `CREATED`) |

**State machine:**
```
CREATED ──assign──► ASSIGNED ──accept──► ACCEPTED ──pickup──► PICKED_UP ──deliver──► DELIVERED
   │                    │
   │              reject (back to CREATED)
   │
   └──cancel──► CANCELLED
```

---

## Pricing Formula

```
price = baseFare + (distance_km × perKmRate)
```

| Config key | Default |
|------------|---------|
| `app.pricing.base-fare` | 2.50 |
| `app.pricing.per-km-rate` | 0.75 |

Distance is calculated using the **Haversine formula** (great-circle distance).  
Example: 20 km → `2.50 + (20 × 0.75)` = **17.50**

---

## WebSocket Topics (STOMP)

Connect endpoint: `ws://localhost:8080/ws`  
SockJS fallback: `http://localhost:8080/ws`

| Topic | Payload | Description |
|-------|---------|-------------|
| `/topic/orders/new` | `OrderResponse` | Broadcast when a new order is created (subscribe as rider) |
| `/topic/orders/{orderId}` | `OrderResponse` | Status change for a specific order |
| `/topic/riders/{riderId}/location` | `RiderLocationResponse` | Live GPS update |

---

---

# AUTH ENDPOINTS

## POST /api/auth/register

Register a new user. Returns an OTP (mock value in dev).

**Auth:** None

**Request:**
```json
{
  "name":  "Doniel Tripura",
  "phone": "+8801700000001",
  "role":  "CUSTOMER"
}
```

| Field | Type | Constraints |
|-------|------|-------------|
| `name` | string | 2–100 chars, required |
| `phone` | string | E.164 format `^\+?[1-9]\d{6,14}$`, required |
| `role` | `Role` | `CUSTOMER` or `RIDER`, required |

**Response `201`:**
```json
{
  "status": 201,
  "data": {
    "message": "Registration successful. OTP sent.",
    "phone":   "+8801700000001",
    "otp":     "3842"
  }
}
```
> `otp` is only returned when `app.otp.mock-enabled: true` (dev mode). Remove in production.

---

## POST /api/auth/send-otp

Request a fresh OTP for an existing user (subsequent logins).

**Auth:** None

**Request:**
```json
{ "phone": "+8801700000001" }
```

**Response `200`:**
```json
{
  "status": 200,
  "data": {
    "message": "OTP sent.",
    "phone":   "+8801700000001",
    "otp":     "1234"
  }
}
```

---

## POST /api/auth/login

Verify OTP and receive JWT tokens.

**Auth:** None

**Request:**
```json
{
  "phone": "+8801700000001",
  "otp":   "3842"
}
```

**Response `200`:**
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

| Field | Description |
|-------|-------------|
| `accessToken` | JWT — valid 15 min (900 s) |
| `refreshToken` | JWT — valid 7 days |
| `expiresIn` | Access token TTL in **seconds** |

---

## POST /api/auth/refresh

Rotate an expired access token using a valid refresh token.

**Auth:** None

**Request:**
```json
{ "refreshToken": "eyJhbGci..." }
```

**Response `200`:** Same shape as `/login`.

> Refresh tokens are **single-use** and rotate on each call. The old token is invalidated.

---

---

# ORDER ENDPOINTS

All order endpoints require a valid JWT.

## POST /api/orders

Place a new delivery order.

**Auth:** `CUSTOMER` role required

**Request:**
```json
{
  "pickupLat": 23.9000,
  "pickupLng": 90.4000,
  "dropLat":   23.8103,
  "dropLng":   90.4125,
  "type":      "PARCEL",
  "note":      "Fragile — handle with care"
}
```

| Field | Type | Constraints |
|-------|------|-------------|
| `pickupLat` | double | -90 to 90, required |
| `pickupLng` | double | -180 to 180, required |
| `dropLat` | double | -90 to 90, required |
| `dropLng` | double | -180 to 180, required |
| `type` | `OrderType` | `DOCUMENT`, `SMALL`, or `PARCEL`, required |
| `note` | string | max 500 chars, optional |

**Response `201`:** `OrderResponse` (see schema below)

---

## GET /api/orders

List all orders placed by the authenticated customer.

**Auth:** `CUSTOMER` role required

**Response `200`:** Array of `OrderResponse`

---

## GET /api/orders/{id}

Get a specific order by ID.

**Auth:** Any authenticated user

**Path param:** `id` — UUID

**Response `200`:** `OrderResponse`

---

## POST /api/orders/{id}/cancel

Cancel an order. Only allowed when status is `CREATED`.

**Auth:** `CUSTOMER` role required (must own the order)

**Response `200`:** `OrderResponse` with `status: CANCELLED`

**Errors:**
- `400` — Order is not in `CREATED` state
- `400` — Order does not belong to you

---

## POST /api/orders/{id}/assign

Find and assign the nearest online rider to this order.

**Auth:** `CUSTOMER` role required (must own the order)

**Algorithm:**
1. Filters all riders with `isOnline = true` and a known GPS location
2. Calculates Haversine distance from each rider to the pickup point
3. Assigns the closest rider

**Response `200`:** `OrderResponse` with `status: ASSIGNED`, `riderId`, `riderName`, `assignedAt`

**Errors:**
- `400` — Order is not in `CREATED` state
- `400` — No online riders available
- `400` — Order does not belong to you

---

## POST /api/orders/{id}/accept

Assigned rider accepts the order.

**Auth:** `RIDER` role required (must be the assigned rider)

**Response `200`:** `OrderResponse` with `status: ACCEPTED`

**Errors:**
- `400` — Order is not in `ASSIGNED` state
- `400` — Order is not assigned to you
- `400` — Rider must be online to accept orders

---

## POST /api/orders/{id}/reject

Assigned rider rejects the order. Order reverts to `CREATED` and the rider is unassigned.

**Auth:** `RIDER` role required (must be the assigned rider)

**Response `200`:** `OrderResponse` with `status: CREATED`, `riderId: null`

**Errors:**
- `400` — Order is not in `ASSIGNED` state
- `400` — Order is not assigned to you

---

## POST /api/orders/{id}/pickup

Rider marks the item as collected from sender.

**Auth:** `RIDER` role required (must be assigned rider)

**Response `200`:** `OrderResponse` with `status: PICKED_UP`, `pickedUpAt` set

**Errors:**
- `400` — Order is not in `ACCEPTED` state
- `400` — Order is not assigned to you

---

## POST /api/orders/{id}/deliver

Rider marks the order as delivered.

**Auth:** `RIDER` role required (must be assigned rider)

**Response `200`:** `OrderResponse` with `status: DELIVERED`, `deliveredAt` set

**Errors:**
- `400` — Order is not in `PICKED_UP` state
- `400` — Order is not assigned to you

---

---

# RIDER ENDPOINTS

All rider endpoints require `RIDER` role JWT.

## POST /api/riders/online

Mark rider as available for dispatch.

**Auth:** `RIDER` role required

**Response `200`:** `{}`

---

## POST /api/riders/offline

Mark rider as unavailable. Also clears Redis location entry.

**Auth:** `RIDER` role required

**Response `200`:** `{}`

---

## POST /api/riders/location

Push current GPS coordinates. Stored in:
- PostgreSQL `rider_profiles.current_lat / current_lng` (used for nearest-rider query)
- Redis `rider_location:{riderId}` with 5-min TTL (used for real-time tracking)
- WebSocket `/topic/riders/{riderId}/location` (broadcast to customer)

**Auth:** `RIDER` role required

**Request:**
```json
{
  "lat": 23.9010,
  "lng": 90.4010
}
```

| Field | Type | Constraints |
|-------|------|-------------|
| `lat` | double | -90 to 90, required |
| `lng` | double | -180 to 180, required |

**Response `200`:** `{}`

---

## POST /api/riders/status *(legacy)*

Toggle online/offline using a body flag. Kept for backward compatibility.

**Request:**
```json
{ "online": true }
```

---

---

# RESPONSE SCHEMAS

## OrderResponse

```json
{
  "id":           "uuid",
  "customerId":   "uuid",
  "customerName": "string",
  "riderId":      "uuid | null",
  "riderName":    "string | null",

  "pickupLat":    0.0,
  "pickupLng":    0.0,
  "dropLat":      0.0,
  "dropLng":      0.0,

  "type":         "DOCUMENT | SMALL | PARCEL",
  "status":       "CREATED | ASSIGNED | ACCEPTED | PICKED_UP | DELIVERED | CANCELLED",
  "price":        0.00,
  "note":         "string | null",

  "assignedAt":   "ISO-8601 | null",
  "pickedUpAt":   "ISO-8601 | null",
  "deliveredAt":  "ISO-8601 | null",

  "createdAt":    "ISO-8601",
  "updatedAt":    "ISO-8601"
}
```

## RiderLocationResponse

```json
{
  "riderId":   "uuid",
  "lat":       0.0,
  "lng":       0.0,
  "timestamp": "ISO-8601"
}
```

## AuthResponse

```json
{
  "accessToken":  "string",
  "refreshToken": "string",
  "expiresIn":    900
}
```

---

---

# ERROR CODES

| HTTP | Error | When |
|------|-------|------|
| `400` | `Bad Request` | Business rule violation, invalid state transition |
| `400` | `Validation Failed` | Missing or invalid request fields |
| `401` | `Unauthorized` | No token, invalid token, expired OTP |
| `403` | `Forbidden` | Wrong role for endpoint |
| `404` | `Not Found` | Order / user / rider profile does not exist, unknown route |
| `500` | `Internal Server Error` | Unexpected server fault |

**Error response shape:**
```json
{
  "status":  400,
  "error":   "Bad Request",
  "message": "Order can only be cancelled when status is CREATED. Current status: ASSIGNED",
  "path":    "/api/orders/uuid/cancel",
  "timestamp": "2026-05-02T12:00:00Z"
}
```

---

---

# SECURITY NOTES

- All timestamps are UTC ISO-8601
- OTPs are 4-digit, valid for **5 minutes**, single-use
- Access tokens expire in **15 minutes** — use `/api/auth/refresh` to rotate
- Refresh tokens expire in **7 days** and rotate on each use
- Phone numbers are stored as-is after trim; use E.164 format (`+8801700000001`)

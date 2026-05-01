# 🚚 Do Delivery Backend — Test Guide (MVP Flow)

This document helps **developers, testers, and new contributors** quickly verify the backend system step-by-step using `curl` or Postman.

---

# 📌 Base URL

```
http://localhost:8080
```

---

# 🔐 1. AUTH FLOW

## 1.1 Register User

```bash
curl -X POST http://localhost:8080/api/auth/register \
-H "Content-Type: application/json" \
-d '{
  "name": "Doniel",
  "phone": "+8801700000000",
  "role": "CUSTOMER"
}'
```

### Expected Response

* `201 CREATED`
* User created successfully

---

## 1.2 Send OTP

```bash
curl -X POST http://localhost:8080/api/auth/send-otp \
-H "Content-Type: application/json" \
-d '{
  "phone": "+8801700000000"
}'
```

### Expected Response

* OTP sent

---

## 1.3 Login (Get JWT)

```bash
curl -X POST http://localhost:8080/api/auth/login \
-H "Content-Type: application/json" \
-d '{
  "phone": "+8801700000000",
  "otp": "1234"
}'
```

### Expected Response

* `accessToken`
* `refreshToken`

---

# 🚚 2. ORDER FLOW

## 2.1 Create Order

```bash
curl -X POST http://localhost:8080/api/orders \
-H "Content-Type: application/json" \
-H "Authorization: Bearer <ACCESS_TOKEN>" \
-d '{
  "pickupLat": 23.9999,
  "pickupLng": 90.4203,
  "dropLat": 23.8103,
  "dropLng": 90.4125,
  "type": "PARCEL",
  "note": "Pickup from Kaliganj, drop at Dhaka"
}'
```

### Expected Response

* `201 CREATED`
* Order created with:

  * status = `CREATED`
  * customerId filled

---

## 2.2 Get My Orders

```bash
curl -X GET http://localhost:8080/api/orders \
-H "Authorization: Bearer <ACCESS_TOKEN>"
```

### Expected Response

* List of user orders

---

## 2.3 Get Order by ID

```bash
curl -X GET http://localhost:8080/api/orders/<ORDER_ID> \
-H "Authorization: Bearer <ACCESS_TOKEN>"
```

---

# 🔐 3. SECURITY TESTS

## 3.1 No Token (Should Fail)

```bash
curl -X GET http://localhost:8080/api/orders
```

### Expected

* `401 Unauthorized`

---

## 3.2 Wrong Role Access

Try accessing customer endpoint with RIDER token.

### Expected

* `403 Forbidden`

---

# 🚨 4. SYSTEM RULES

## Order Status Flow

```
CREATED → ASSIGNED → ACCEPTED → PICKED_UP → DELIVERED
```

---

# 🧪 5. TROUBLESHOOTING

## If 401 Unauthorized

* Check JWT token format
* Ensure header:

```
Authorization: Bearer <token>
```

## If 500 Error

* Check backend logs
* Most likely service or DB issue

---

# 🚀 6. NEXT MODULE (FUTURE)

## Rider System (Coming Next)

* Rider registration
* Rider online/offline
* Assign nearest rider
* Accept / reject order
* Real-time updates (WebSocket)

---

# 👨‍💻 Maintainer Notes

This system is currently MVP-level and stable for:

* Authentication
* Order creation
* Basic order retrieval

Further improvements should focus on:

* Rider dispatch system
* Real-time tracking
* Location-based assignment

---

# 📦 Get Order by ID

## Request

```bash
curl -X GET http://localhost:8080/api/orders/<ORDER_ID> \
-H "Authorization: Bearer <ACCESS_TOKEN>"
```

## Example Request

```bash
curl -X GET http://localhost:8080/api/orders/6cccc9f5-2ffb-4b0c-a2d2-19a7e73b8fde \
-H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

## ✅ Sample Response

```json
{
    "status": 200,
    "data": {
        "id": "6cccc9f5-2ffb-4b0c-a2d2-19a7e73b8fde",
        "customerId": "a944ebea-30a3-4906-86ed-c7c7c917040d",
        "customerName": "Doniel",
        "riderId": null,
        "riderName": null,
        "pickupLat": 23.9999,
        "pickupLng": 90.4203,
        "dropLat": 23.8103,
        "dropLng": 90.4125,
        "type": "PARCEL",
        "status": "CREATED",
        "price": 18.32,
        "note": "Pickup from Kaliganj, drop at Dhaka",
        "createdAt": "2026-05-01T17:21:00.484035Z",
        "updatedAt": "2026-05-01T17:21:00.484071Z"
    },
    "timestamp": "2026-05-01T17:32:22.537733Z"
}
```

---

# 📦 Get My Orders

## Request

```bash
curl -X GET http://localhost:8080/api/orders \
-H "Authorization: Bearer <ACCESS_TOKEN>"
```

## Example Response

```json
{
    "status": 200,
    "data": [
        {
            "id": "6cccc9f5-2ffb-4b0c-a2d2-19a7e73b8fde",
            "customerId": "a944ebea-30a3-4906-86ed-c7c7c917040d",
            "customerName": "Doniel",
            "riderId": null,
            "riderName": null,
            "pickupLat": 23.9999,
            "pickupLng": 90.4203,
            "dropLat": 23.8103,
            "dropLng": 90.4125,
            "type": "PARCEL",
            "status": "CREATED",
            "price": 18.32,
            "note": "Pickup from Kaliganj, drop at Dhaka",
            "createdAt": "2026-05-01T17:21:00.484035Z",
            "updatedAt": "2026-05-01T17:21:00.484071Z"
        }
    ],
    "timestamp": "2026-05-01T17:30:42.523797Z"
}
```

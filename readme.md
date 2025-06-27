## Cache Simulator

This project simulates various cache architectures through a full-stack web application. It supports **Direct-Mapped**, **Set-Associative**, and **Fully-Associative** caches with configurable policies. The frontend provides an interactive interface, and the backend performs cache logic and data handling.

## Table of Contents

* [Cache Simulator](#-cache-simulator)
* [Features](#-features)
* [Tech Stack](#-tech-stack)
* [Project Structure](#-project-structure)
* [Setup Instructions](#️-setup-instructions)

  * [Backend Setup](#️-backend-setup)
  * [Frontend Setup](#-frontend-setup)
* [API Endpoints](#-api-endpoints)
* [Example API Request](#-example-api-request)
* [Github Link](#-github-link)
* [Contributors](#-contributors)


## Features

* Simulate cache read/write operations
* Support for:

  * Direct Mapped
  * Set-Associative 
  * Fully Associative
* Replacement policies:

  * **FIFO**
  * **LRU**
  * **LFU**
  * **RANDOM**
* Write policies:

  * **Write-Through**
  * **Write-Back**
  * **Write-No-Allocate**
  * **Write-Allocate**
* Memory visualization
* Configurable:

  * Cache size
  * Block size
  * Word size
  * Number of ways
  * Replacement Policy
  * Write Policy
* Detailed response showing cache state, block data, hit/miss, and more


## Tech Stack

| Layer         | Technology             |
| ------------- | ---------------------- |
| Frontend      | React.js, Tailwind CSS |
| Backend       | Java (Spring Boot)     |
| Communication | REST API (JSON)        |

## Project Structure

```
cache-simulator/
│
├── backend/                # Spring Boot application
│   ├── controller/         # API endpoints
│   ├── model/              # Core logic: cache, memory, block
│   ├── dto/                # Request/response structures
│   └── CacheApplication.java
│
├── frontend/               # React frontend
│   ├── src/
│   │   ├── components/     # UI components
│   │   ├── api/            # Axios-based API hooks
│   │   └── App.jsx         # Entry point
│
└── README.md
```

## Setup Instructions

### Prerequisites

* Java 17+ (for backend)
* Node.js 16+ and npm/yarn (for frontend)


### Backend Setup

```bash
cd backend/
./mvnw spring-boot:run
```

By default, the server runs on: `http://localhost:8080`

### Frontend Setup

```bash
cd frontend/
npm install
npm run dev
```

Frontend runs on: `http://localhost:5173`

## API ENDPOINTS

All endpoints are prefixed with `http://localhost:8080/api/cache`.

```
| Method | Endpoint     | Description                      |
| ------ | ------------ | -------------------------------- |
| GET    | `/ping`      | Health check / ping              |
| POST   | `/configure` | Configure cache with parameters  |
| POST   | `/request`   | Perform cache read/write request |

```

### `POST /configure`

**Purpose:**
Configure the cache architecture before sending requests.

**Payload Structure:**

```json
{
  "cacheType": "ASSOCIATIVE",             // Options: DIRECT, SET-ASSOCIATIVE, ASSOCIATIVE
  "cacheSize": 64,                        // In bytes
  "blockSize": 16,                        // In bytes
  "ways": 4,                              // Used only if SET-ASSOCIATIVE else any value
  "writePolicyOnHit": "WRITE-THROUGH",    // Options: WRITE-THROUGH, WRITE-BACK
  "writePolicyOnMiss": "WRITE-ALLOCATE",  // Options: WRITE-ALLOCATE, WRITE-NO-ALLOCATE
  "replacementPolicy": "LFU",             // Options: LRU, FIFO, LFU, Random
  "wordSize": 8                           // In bytes 
}
```

### `POST /request`

**Purpose:**
Read from or write to the cache (after configuration).

**Payload Structure:**

```json
{
  "address": 0,         // Memory Address
  "action": "read",     // "read" or "write"
  "data": [77]          // Required for write; ignored for read
}
```
<br>

## Example API Request

```json
POST /request

{
  "address": 0,
  "action": "read",
  "data": [86]
}
```

### Response Sample

```json
{
  "cacheFinal": [51, 52, 53, 54],
  "memoryIndex": 2,
  "memoryData": [86],
  "type": "read",
  "index": 1,
  "tag": 3,
  "offset": 0,
  "block": 2,
  "data": 86,
  "hit": false,
  "oldState": "INVALID",
  "newState": "MODIFIED",
  "wordSize": 4
}
```
## Github Link
* [Frontend](https://github.com/sauravatgithub-web/Cache-Visualizer.git)
* [Backend](https://github.com/pntu007/cache_api.git)

## Contributors

* Saurav Singh
* Harsh Maurya 
